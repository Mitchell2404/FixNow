package com.fixnow.app.data.repository

import com.fixnow.app.core.util.safeCall
import com.fixnow.app.data.mapper.toFriendlyMessage
import com.fixnow.app.domain.model.EstadoSolicitud
import com.fixnow.app.domain.model.SolicitudServicio
import com.fixnow.app.domain.repository.SolicitudRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.fixnow.app.data.mapper.toSolicitudServicio
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source

@Singleton
class SolicitudRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
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

        documento.set(datos).await()

        documento.id
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
}