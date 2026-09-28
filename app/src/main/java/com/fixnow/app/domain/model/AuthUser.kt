package com.fixnow.app.domain.model

/** Usuario autenticado, sin depender de las clases de Firebase. */
data class AuthUser(
    val uid: String,
    val email: String,
    val isEmailVerified: Boolean
)
