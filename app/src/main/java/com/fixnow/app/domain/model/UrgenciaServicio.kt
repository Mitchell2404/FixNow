package com.fixnow.app.domain.model

/**
 * Recargos por urgencia.
 */
enum class UrgenciaServicio(
    val titulo: String,
    val descripcion: String,
    val recargo: Double
) {
    NORMAL(
        titulo = "Normal",
        descripcion = "Puedo coordinar la atención con tiempo.",
        recargo = 0.0
    ),
    PRIORITARIA(
        titulo = "Prioritaria",
        descripcion = "El problema limita el uso de mi equipo.",
        recargo = 15.0
    ),
    URGENTE(
        titulo = "Urgente",
        descripcion = "Necesito atención lo antes posible.",
        recargo = 30.0
    )
}