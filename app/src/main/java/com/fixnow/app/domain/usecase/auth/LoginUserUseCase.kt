package com.fixnow.app.domain.usecase.auth

import com.fixnow.app.domain.model.AuthResult
import com.fixnow.app.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): AuthResult =
        authRepository.login(email, password)
}
