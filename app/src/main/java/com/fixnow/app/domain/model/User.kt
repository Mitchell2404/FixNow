package com.fixnow.app.domain.model

/**
 * Perfil de usuario. Se guarda en Firestore, colección "users",
 * con el UID de Firebase Auth como ID del documento.
 *
 * Todos los campos tienen valor por defecto: Firestore lo necesita para
 * poder convertir el documento a este objeto.
 */
data class User(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    /** Foto de perfil reducida (256 px) en Base64. Se guarda en Firestore para no depender de Firebase Storage. */
    val fotoBase64: String = "",
    val rol: String = ROL_CLIENTE,
    val notificacionesActivas: Boolean = true,
    val biometriaActivada: Boolean = false,
    val creadoEn: Long = System.currentTimeMillis()
) {
    companion object {
        const val ROL_CLIENTE = "cliente"
        const val ROL_TECNICO = "tecnico"
        const val ROL_ADMIN = "admin"
    }
}
