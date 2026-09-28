package com.fixnow.app.domain.usecase.auth

import com.fixnow.app.domain.model.AuthResult
import com.fixnow.app.domain.repository.AuthRepository
import com.fixnow.app.domain.repository.ProfileRepository
import javax.inject.Inject

/** HU04: crea la cuenta en Firebase Auth y el documento inicial del perfil en Firestore. */
class RegisterUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(email: String, password: String): AuthResult {
        val result = authRepository.register(email, password)
        if (result is AuthResult.Success) {
            profileRepository.createInitialProfile(
                uid = result.user.uid,
                email = result.user.email.ifBlank { email }
            )
        }
        return result
    }
}
