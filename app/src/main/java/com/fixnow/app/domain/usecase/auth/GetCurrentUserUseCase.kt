package com.fixnow.app.domain.usecase.auth

import com.fixnow.app.domain.model.AuthUser
import com.fixnow.app.domain.repository.AuthRepository
import javax.inject.Inject

/** HU04 - sesión persistente: devuelve el usuario con sesión guardada, o null. */
class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): AuthUser? = authRepository.currentUser
}
