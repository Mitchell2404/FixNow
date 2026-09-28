package com.fixnow.app.domain.usecase.biometric

import com.fixnow.app.domain.repository.BiometricSettingsRepository
import javax.inject.Inject

class IsBiometricEnabledUseCase @Inject constructor(
    private val settings: BiometricSettingsRepository
) {
    operator fun invoke(): Boolean = settings.isEnabled()
}
