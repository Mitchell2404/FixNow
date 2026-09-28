package com.fixnow.app.domain.usecase.profile

import com.fixnow.app.domain.model.User
import com.fixnow.app.domain.repository.ProfileRepository
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(uid: String): Result<User> = profileRepository.getProfile(uid)
}
