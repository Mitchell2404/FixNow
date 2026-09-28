package com.fixnow.app.domain.usecase.auth

import com.fixnow.app.domain.model.AuthUser
import com.fixnow.app.domain.repository.AuthRepository
import javax.inject.Inject

/** Vuelve a pedir los datos del usuario a Firebase (por ejemplo, para saber si ya verificó su correo). */
class RefreshCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): AuthUser? = authRepository.refreshCurrentUser()
}
