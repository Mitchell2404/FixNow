package com.fixnow.app.domain.usecase.auth

import com.fixnow.app.domain.repository.AuthRepository
import javax.inject.Inject

class SendPasswordResetUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> = authRepository.sendPasswordReset(email)
}
