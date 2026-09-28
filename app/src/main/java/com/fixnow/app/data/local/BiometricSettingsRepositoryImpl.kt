package com.fixnow.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fixnow.app.domain.repository.BiometricSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HU06 - guarda, cifrado y solo en ESTE dispositivo, si el usuario activó la huella.
 * Si el Keystore del teléfono falla, se asume "desactivado" en vez de cerrar la app.
 */
@Singleton
class BiometricSettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BiometricSettingsRepository {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun isEnabled(): Boolean =
        runCatching { prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false) }.getOrDefault(false)

    override fun setEnabled(enabled: Boolean) {
        runCatching { prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply() }
    }

    private companion object {
        const val FILE_NAME = "fixnow_secure_prefs"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }
}
