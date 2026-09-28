package com.fixnow.app.domain.usecase.profile

import com.fixnow.app.domain.repository.ProfileRepository
import javax.inject.Inject

class SaveProfilePhotoUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(uid: String, imageUri: String): Result<String> =
        profileRepository.saveProfilePhoto(uid, imageUri)
}
