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
}