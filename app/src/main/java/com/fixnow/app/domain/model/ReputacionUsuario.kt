package com.fixnow.app.domain.model

data class ReputacionUsuario(
    val uid: String = "",
    val promedio: Double = 0.0,
    val totalCalificaciones: Int = 0,
    val ultimaValoracion: Long = 0L
)
