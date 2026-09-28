package com.fixnow.app.domain.repository

import com.fixnow.app.domain.model.AuthResult
import com.fixnow.app.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

/** Contrato de autenticación. La implementación real (Firebase) vive en la capa data. */
interface AuthRepository {
    val currentUser: AuthUser?
    fun observeAuthState(): Flow<AuthUser?>
    suspend fun register(email: String, password: String): AuthResult
    suspend fun login(email: String, password: String): AuthResult
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun resendVerificationEmail(): Result<Unit>
    suspend fun refreshCurrentUser(): AuthUser?
    fun logout()
}
