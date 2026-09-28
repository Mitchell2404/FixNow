package com.fixnow.app.domain.usecase.biometric

import com.fixnow.app.domain.repository.BiometricSettingsRepository
import com.fixnow.app.domain.repository.ProfileRepository
import javax.inject.Inject

/** HU06: activar/desactivar el ingreso con huella. */
class SetBiometricEnabledUseCase @Inject constructor(
    private val settings: BiometricSettingsRepository,
    private val profileRepository: ProfileRepository
) {
    /**
     * Guarda primero en el dispositivo (lo que importa para el login) y luego
     * en Firestore. El resultado que se devuelve es el de Firestore.
     */
    suspend operator fun invoke(uid: String, enabled: Boolean): Result<Unit> {
        settings.setEnabled(enabled)
        return profileRepository.setBiometricEnabled(uid, enabled)
    }
}
