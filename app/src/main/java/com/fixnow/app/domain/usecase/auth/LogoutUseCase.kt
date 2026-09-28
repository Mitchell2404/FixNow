package com.fixnow.app.domain.usecase.auth

import com.fixnow.app.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke() = authRepository.logout()
}
