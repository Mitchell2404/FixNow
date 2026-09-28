package com.fixnow.app.data.mapper

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException

/** Traduce errores técnicos de Firebase a mensajes que el usuario entienda. */
fun Throwable.toFriendlyMessage(): String = when (this) {
    // WeakPassword hereda de InvalidCredentials, por eso va primero.
    is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil (mínimo 6 caracteres)"
    is FirebaseAuthUserCollisionException -> "Ese correo ya está registrado"
    is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo"
    is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos"
    is FirebaseNetworkException -> "Sin conexión a internet"
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.UNAVAILABLE -> "Sin conexión a internet"
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> "No tienes permisos para esta acción"
        else -> message ?: "Ocurrió un error inesperado"
    }
    else -> message ?: "Ocurrió un error inesperado"
}
