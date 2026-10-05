package com.fixnow.app.domain.repository

import com.fixnow.app.domain.model.SolicitudServicio

interface SolicitudRepository {

    // Devuelve el identificador de la solicitud registrada.
    suspend fun crearSolicitud(
        solicitud: SolicitudServicio
    ): Result<String>
}