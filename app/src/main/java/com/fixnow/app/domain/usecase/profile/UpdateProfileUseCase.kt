package com.fixnow.app.domain.usecase.profile

import com.fixnow.app.domain.repository.ProfileRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(uid: String, nombre: String, notificacionesActivas: Boolean): Result<Unit> =
        profileRepository.updateProfile(uid, nombre, notificacionesActivas)
}
