package com.fixnow.app.data.repository

import android.content.Context
import androidx.annotation.OptIn
import com.fixnow.app.core.util.safeCall
import com.fixnow.app.data.local.PendingSolicitudQueue
import com.fixnow.app.data.mapper.toFriendlyMessage
import com.fixnow.app.domain.model.EstadoSolicitud
import com.fixnow.app.domain.model.ReputacionUsuario
import com.fixnow.app.domain.model.SolicitudServicio
import com.fixnow.app.domain.repository.SolicitudRepository
import com.fixnow.app.work.SolicitudSyncScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.fixnow.app.data.mapper.toSolicitudServicio
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class SolicitudRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : SolicitudRepository {

    override suspend fun crearSolicitud(
        solicitud: SolicitudServicio
    ): Result<String> = safeCall(Throwable::toFriendlyMessage) {

        val usuario = auth.currentUser
            ?: error("Debes iniciar sesión para solicitar un servicio.")

        val documento = firestore
            .collection("solicitudes")
            .document()

        val datos = hashMapOf<String, Any?>(
            "id" to documento.id,
            "clienteId" to usuario.uid,

            "categoriaId" to solicitud.categoriaId,
            "categoriaNombre" to solicitud.categoriaNombre,
            "descripcion" to solicitud.descripcion.trim(),

            "latitud" to solicitud.latitud,
            "longitud" to solicitud.longitud,
            "direccion" to solicitud.direccion.trim(),

            "fotoBase64" to solicitud.fotoBase64,

            "urgencia" to solicitud.urgencia,
            "precioSugerido" to solicitud.precioSugerido,

            "estado" to EstadoSolicitud.PUBLICADA.name,
            "tecnicoId" to null,

            "fechaCreacion" to FieldValue.serverTimestamp()
        )

        try {
            documento.set(datos).await()
            documento.id
        } catch (error: FirebaseFirestoreException) {
            if (
                error.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                error.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ||
                error.code == FirebaseFirestoreException.Code.CANCELLED
            ) {
                val pending = solicitud.copy(
                    id = documento.id,
                    clienteId = usuario.uid,
                    estado = EstadoSolicitud.PUBLICADA.name
                )
                PendingSolicitudQueue(context).enqueue(pending)
                SolicitudSyncScheduler.enqueue(context)
                documento.id
            } else {
                throw error
            }
        }
    }

    override suspend fun obtenerMisSolicitudes():
            Result<List<SolicitudServicio>> =
        safeCall(Throwable::toFriendlyMessage) {

            val usuario = auth.currentUser
                ?: error("Debes iniciar sesión para ver tus solicitudes.")

            val resultado = firestore
                .collection("solicitudes")
                .whereEqualTo("clienteId", usuario.uid)
                .orderBy(
                    "fechaCreacion",
                    Query.Direction.DESCENDING
                )
                .get(Source.SERVER)
                .await()

            resultado.documents.map { documento ->
                documento.toSolicitudServicio()
            }
        }

    override fun observarMisSolicitudes(): Flow<List<SolicitudServicio>> = callbackFlow {
        val usuario = auth.currentUser
        if (usuario == null) {
            close(IllegalStateException("Debes iniciar sesión para ver tus solicitudes."))
            return@callbackFlow
        }
        val listener = firestore.collection("solicitudes")
            .whereEqualTo("clienteId", usuario.uid)
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) close(error)
                else if (snapshot != null) trySend(snapshot.documents.map { it.toSolicitudServicio() })
            }
        awaitClose { listener.remove() }
    }

    override fun observarSolicitudesDisponibles(): Flow<List<SolicitudServicio>> =
        callbackFlow {
            val listener = firestore
                .collection("solicitudes")
                .whereEqualTo("estado", EstadoSolicitud.PUBLICADA.name)
                .whereEqualTo("tecnicoId", null)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                    } else if (snapshot != null) {
                        trySend(snapshot.documents.map { it.toSolicitudServicio() })
                    }
                }

            awaitClose { listener.remove() }
        }

    override fun observarSolicitudesTecnico(): Flow<List<SolicitudServicio>> = callbackFlow {
        val usuario = auth.currentUser
        if (usuario == null) {
            close(IllegalStateException("Debes iniciar sesión para ver tus solicitudes."))
            return@callbackFlow
        }
        val listener = firestore.collection("solicitudes")
            .whereEqualTo("tecnicoId", usuario.uid)
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) close(error)
                else if (snapshot != null) trySend(snapshot.documents.map { it.toSolicitudServicio() })
            }
        awaitClose { listener.remove() }
    }

    override suspend fun aceptarSolicitud(solicitudId: String): Result<Unit> =
        safeCall(Throwable::toFriendlyMessage) {
            require(solicitudId.isNotBlank()) { "No se pudo identificar la solicitud." }
            val usuario = auth.currentUser ?: error("Debes iniciar sesión.")
            val referencia = firestore.collection("solicitudes").document(solicitudId)
            firestore.runTransaction { transaccion ->
                val solicitud = transaccion.get(referencia)
                check(solicitud.exists()) { "La solicitud ya no existe." }
                check(solicitud.getString("estado") == EstadoSolicitud.PUBLICADA.name &&
                    solicitud.getString("tecnicoId") == null) {
                    "Otro técnico ya aceptó esta solicitud. Actualiza la lista."
                }
                transaccion.update(
                    referencia,
                    mapOf(
                        "estado" to EstadoSolicitud.ACEPTADA.name,
                        "tecnicoId" to usuario.uid,
                        "comentarioEstado" to ""
                    )
                )
                Unit
            }.await()
            Unit
        }

    override suspend fun actualizarSolicitudTecnico(
        solicitudId: String,
        estado: EstadoSolicitud,
        comentario: String
    ): Result<Unit> = safeCall(Throwable::toFriendlyMessage) {
        require(solicitudId.isNotBlank()) { "No se pudo identificar la solicitud." }
        val usuario = auth.currentUser ?: error("Debes iniciar sesión.")
        val comentarioLimpio = comentario.trim().take(500)
        val referencia = firestore.collection("solicitudes").document(solicitudId)
        firestore.runTransaction { transaccion ->
            val solicitud = transaccion.get(referencia)
            check(solicitud.exists()) { "La solicitud ya no existe." }
            check(solicitud.getString("tecnicoId") == usuario.uid) {
                "Solo el técnico asignado puede actualizar esta solicitud."
            }
            val estadoActual = solicitud.getString("estado")
            val transicionValida = when (estadoActual) {
                EstadoSolicitud.ACEPTADA.name -> estado in setOf(
                    EstadoSolicitud.EN_PROCESO,
                    EstadoSolicitud.CANCELADA
                )
                EstadoSolicitud.EN_PROCESO.name -> estado in setOf(
                    EstadoSolicitud.FINALIZADA,
                    EstadoSolicitud.CANCELADA
                )
                else -> false
            }
            check(transicionValida || (estadoActual == estado.name && comentarioLimpio.isNotBlank())) {
                "No se puede cambiar la solicitud desde su estado actual."
            }
            val cambios = mutableMapOf<String, Any>("estado" to estado.name)
            if (comentarioLimpio.isNotBlank()) cambios["comentarioEstado"] = comentarioLimpio
            transaccion.update(referencia, cambios)
            Unit
        }.await()
        Unit
    }

    override suspend fun registrarUbicacionTecnico(
        latitud: Double,
        longitud: Double
    ): Result<Unit> = safeCall(Throwable::toFriendlyMessage) {
        require(latitud in -90.0..90.0 && longitud in -180.0..180.0) {
            "La ubicación recibida no es válida."
        }
        val usuario = auth.currentUser ?: error("Debes iniciar sesión.")
        firestore.collection("users").document(usuario.uid).set(
            mapOf(
                "ubicacionLatitud" to latitud,
                "ubicacionLongitud" to longitud,
                "ubicacionActualizadaEn" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
        Unit
    }

    override suspend fun registrarTokenTecnico(token: String): Result<Unit> =
        safeCall(Throwable::toFriendlyMessage) {
            require(token.isNotBlank()) { "El token de notificaciones no es válido." }
            val usuario = auth.currentUser ?: error("Debes iniciar sesión.")
            firestore.collection("users").document(usuario.uid)
                .collection("fcmTokens").document(token)
                .set(
                    mapOf("token" to token, "actualizadoEn" to FieldValue.serverTimestamp()),
                    SetOptions.merge()
                ).await()
            Unit
        }

    override suspend fun cancelarSolicitud(
        solicitudId: String
    ): Result<Unit> = safeCall(Throwable::toFriendlyMessage) {

        require(solicitudId.isNotBlank()) {
            "No se pudo identificar la solicitud."
        }

        val usuario = auth.currentUser
            ?: error("Debes iniciar sesión.")

        val referencia = firestore
            .collection("solicitudes")
            .document(solicitudId)

        firestore.runTransaction { transaccion ->
            val documento = transaccion.get(referencia)

            check(documento.exists()) {
                "La solicitud ya no existe."
            }

            check(documento.getString("clienteId") == usuario.uid) {
                "No puedes cancelar esta solicitud."
            }

            check(
                documento.getString("estado") ==
                        EstadoSolicitud.PUBLICADA.name &&
                        documento.getString("tecnicoId") == null
            ) {
                "La solicitud ya cambió de estado. Actualiza el listado."
            }

            transaccion.update(
                referencia,
                "estado",
                EstadoSolicitud.CANCELADA.name
            )

            Unit
        }.await()

        Unit
    }

    override suspend fun calificarTecnico(
        solicitudId: String,
        estrellas: Int,
        comentario: String
    ): Result<Unit> = safeCall(Throwable::toFriendlyMessage) {
        require(solicitudId.isNotBlank()) { "No se pudo identificar la solicitud." }
        require(estrellas in 1..5) { "La calificación debe estar entre 1 y 5 estrellas." }

        val usuario = auth.currentUser ?: error("Debes iniciar sesión para calificar.")
        val referencia = firestore.collection("solicitudes").document(solicitudId)

        firestore.runTransaction { transaccion ->
            val documento = transaccion.get(referencia)
            check(documento.exists()) { "La solicitud no existe." }
            check(documento.getString("clienteId") == usuario.uid) {
                "Solo el cliente que creó la solicitud puede calificar al técnico."
            }
            check(documento.getString("estado") in setOf(
                EstadoSolicitud.FINALIZADA.name,
                EstadoSolicitud.CANCELADA.name
            )) {
                "Solo puedes calificar una solicitud finalizada o cancelada."
            }
            check(documento.getBoolean("calificadoPorCliente") != true) {
                "Ya calificaste a este técnico para esta solicitud."
            }

            val tecnicoId = documento.getString("tecnicoId") ?: error("La solicitud no tiene técnico asignado.")
            val comentarioLimpio = comentario.trim().take(500)

            transaccion.update(
                referencia,
                mapOf(
                    "calificacionCliente" to estrellas,
                    "calificadoPorCliente" to true,
                    "calificadoEnCliente" to FieldValue.serverTimestamp()
                )
            )

            val reputacionTecnico = firestore.collection("reputaciones").document(tecnicoId)
            transaccion.set(
                reputacionTecnico,
                mapOf(
                    "uid" to tecnicoId,
                    "puntosReputacion" to FieldValue.increment(estrellas.toLong()),
                    "totalCalificaciones" to FieldValue.increment(1),
                    "ultimaValoracion" to FieldValue.serverTimestamp(),
                    "ultimaSolicitudId" to solicitudId
                ),
                SetOptions.merge()
            )
            if (comentarioLimpio.isNotBlank()) {
                transaccion.set(
                    firestore.collection("users").document(tecnicoId)
                        .collection("valoraciones").document(solicitudId),
                    mapOf(
                        "solicitudId" to solicitudId,
                        "autorUid" to usuario.uid,
                        "estrellas" to estrellas,
                        "comentario" to comentarioLimpio,
                        "creadoEn" to FieldValue.serverTimestamp()
                    )
                )
            }

            Unit
        }.await()

        Unit
    }

    override suspend fun calificarCliente(
        solicitudId: String,
        estrellas: Int,
        comentario: String
    ): Result<Unit> = safeCall(Throwable::toFriendlyMessage) {
        require(solicitudId.isNotBlank()) { "No se pudo identificar la solicitud." }
        require(estrellas in 1..5) { "La calificación debe estar entre 1 y 5 estrellas." }

        val usuario = auth.currentUser ?: error("Debes iniciar sesión para calificar.")
        val referencia = firestore.collection("solicitudes").document(solicitudId)

        firestore.runTransaction { transaccion ->
            val documento = transaccion.get(referencia)
            check(documento.exists()) { "La solicitud no existe." }
            check(documento.getString("tecnicoId") == usuario.uid) {
                "Solo el técnico asignado puede calificar al cliente."
            }
            check(documento.getString("estado") in setOf(
                EstadoSolicitud.FINALIZADA.name,
                EstadoSolicitud.CANCELADA.name
            )) {
                "Solo puedes calificar una solicitud finalizada o cancelada."
            }
            check(documento.getBoolean("calificadoPorTecnico") != true) {
                "Ya calificaste a este cliente en esta solicitud."
            }

            val clienteId = documento.getString("clienteId") ?: error("La solicitud no tiene cliente asociado.")
            val comentarioLimpio = comentario.trim().take(500)

            transaccion.update(
                referencia,
                mapOf(
                    "calificacionTecnico" to estrellas,
                    "calificadoPorTecnico" to true,
                    "calificadoEnTecnico" to FieldValue.serverTimestamp()
                )
            )

            val reputacionCliente = firestore.collection("reputaciones").document(clienteId)
            transaccion.set(
                reputacionCliente,
                mapOf(
                    "uid" to clienteId,
                    "puntosReputacion" to FieldValue.increment(estrellas.toLong()),
                    "totalCalificaciones" to FieldValue.increment(1),
                    "ultimaValoracion" to FieldValue.serverTimestamp(),
                    "ultimaSolicitudId" to solicitudId
                ),
                SetOptions.merge()
            )
            if (comentarioLimpio.isNotBlank()) {
                transaccion.set(
                    firestore.collection("users").document(clienteId)
                        .collection("valoraciones").document(solicitudId),
                    mapOf(
                        "solicitudId" to solicitudId,
                        "autorUid" to usuario.uid,
                        "estrellas" to estrellas,
                        "comentario" to comentarioLimpio,
                        "creadoEn" to FieldValue.serverTimestamp()
                    )
                )
            }

            Unit
        }.await()

        Unit
    }

    override suspend fun obtenerReputacion(uid: String): Result<ReputacionUsuario> =
        safeCall(Throwable::toFriendlyMessage) {
            require(uid.isNotBlank()) { "No se pudo identificar al usuario." }

            val documento = firestore.collection("reputaciones").document(uid).get().await()
            val puntos = documento.getLong("puntosReputacion") ?: 0L
            val total = documento.getLong("totalCalificaciones")?.toInt() ?: 0
            val ultima = documento.getTimestamp("ultimaValoracion")?.toDate()?.time ?: 0L

            ReputacionUsuario(
                uid = uid,
                promedio = if (total > 0) puntos.toDouble() / total else 0.0,
                totalCalificaciones = total,
                ultimaValoracion = ultima
            )
        }
}