package com.fixnow.app.domain.usecase.auth

import com.fixnow.app.domain.model.AuthUser
import com.fixnow.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<AuthUser?> = authRepository.observeAuthState()
}
