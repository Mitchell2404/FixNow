package com.fixnow.app.domain.repository

/** Preferencia local (por dispositivo): ¿el usuario activó el ingreso con huella? */
interface BiometricSettingsRepository {
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)
}
