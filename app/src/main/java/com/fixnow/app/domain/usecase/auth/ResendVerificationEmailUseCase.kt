package com.fixnow.app.domain.usecase.auth

import com.fixnow.app.domain.repository.AuthRepository
import javax.inject.Inject

class ResendVerificationEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> = authRepository.resendVerificationEmail()
}
