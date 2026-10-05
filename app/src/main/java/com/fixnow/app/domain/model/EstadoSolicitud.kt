package com.fixnow.app.domain.model

enum class EstadoSolicitud(
    val titulo: String
) {
    PUBLICADA("Publicada"),
    ACEPTADA("Aceptada"),
    EN_PROCESO("En proceso"),
    FINALIZADA("Finalizada"),
    CANCELADA("Cancelada")
}