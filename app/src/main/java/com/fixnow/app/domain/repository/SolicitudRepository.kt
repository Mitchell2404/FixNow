package com.fixnow.app.domain.repository

import com.fixnow.app.domain.model.EstadoSolicitud
import com.fixnow.app.domain.model.SolicitudServicio
import kotlinx.coroutines.flow.Flow

interface SolicitudRepository {

    // Devuelve el identificador de la solicitud registrada.
    suspend fun crearSolicitud(
        solicitud: SolicitudServicio
    ): Result<String>

    suspend fun obtenerMisSolicitudes(): Result<List<SolicitudServicio>>

    fun observarMisSolicitudes(): Flow<List<SolicitudServicio>>

    fun observarSolicitudesDisponibles(): Flow<List<SolicitudServicio>>

    fun observarSolicitudesTecnico(): Flow<List<SolicitudServicio>>

    suspend fun aceptarSolicitud(solicitudId: String): Result<Unit>

    suspend fun actualizarSolicitudTecnico(
        solicitudId: String,
        estado: EstadoSolicitud,
        comentario: String
    ): Result<Unit>

    suspend fun registrarUbicacionTecnico(latitud: Double, longitud: Double): Result<Unit>

    suspend fun registrarTokenTecnico(token: String): Result<Unit>

    suspend fun cancelarSolicitud(
        solicitudId: String
    ): Result<Unit>

    suspend fun calificarTecnico(
        solicitudId: String,
        estrellas: Int,
        comentario: String
    ): Result<Unit>

    suspend fun calificarCliente(
        solicitudId: String,
        estrellas: Int,
        comentario: String
    ): Result<Unit>

    suspend fun obtenerReputacion(
        uid: String
    ): Result<com.fixnow.app.domain.model.ReputacionUsuario>
}