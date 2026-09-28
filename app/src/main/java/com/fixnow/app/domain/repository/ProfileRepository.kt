package com.fixnow.app.domain.repository

import com.fixnow.app.domain.model.User

interface ProfileRepository {
    suspend fun getProfile(uid: String): Result<User>
    suspend fun createInitialProfile(uid: String, email: String): Result<Unit>
    suspend fun updateProfile(uid: String, nombre: String, notificacionesActivas: Boolean): Result<Unit>

    /**
     * Reduce la foto (recibe la Uri como texto), la guarda en el perfil y devuelve
     * la foto ya procesada en Base64.
     */
    suspend fun saveProfilePhoto(uid: String, imageUri: String): Result<String>

    suspend fun setBiometricEnabled(uid: String, enabled: Boolean): Result<Unit>
}
