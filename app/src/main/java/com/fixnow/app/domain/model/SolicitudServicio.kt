package com.fixnow.app.domain.model

/**
 * Datos de una solicitud de soporte de computadoras.
 */
data class SolicitudServicio(
    val id: String = "",
    val clienteId: String = "",

    val categoriaId: String = "",
    val categoriaNombre: String = "",
    val descripcion: String = "",

    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val direccion: String = "",

    val fotoUrl: String = "",

    val urgencia: String = UrgenciaServicio.NORMAL.name,
    val precioSugerido: Double = 0.0,

    val estado: String = EstadoSolicitud.PUBLICADA.name,
    val tecnicoId: String? = null,

    val fechaCreacion: Long = 0L
)