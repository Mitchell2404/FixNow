package com.fixnow.app.presentation.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * HU06 - Envuelve BiometricPrompt (huella u otro biométrico fuerte).
 * Si el dispositivo no es compatible, [isBiometricAvailable] devuelve false y la app
 * usa solo correo y contraseña (fallback).
 */
object BiometricAuthManager {

    fun isBiometricAvailable(context: Context): Boolean =
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    /**
     * @param onSuccess la huella fue reconocida.
     * @param onCancel el usuario tocó "Usar contraseña" o cerró el diálogo.
     * @param onError falló de verdad (demasiados intentos, sensor bloqueado, etc.).
     *
     * Un intento fallido suelto NO termina el diálogo: Android muestra "no reconocida"
     * y deja reintentar, así que no se avisa nada aquí.
     */
    fun showBiometricPrompt(
        fragment: Fragment,
        title: String = "Inicio de sesión biométrico",
        subtitle: String = "Usa tu huella dactilar para ingresar a FixNow",
        onSuccess: () -> Unit,
        onCancel: () -> Unit = {},
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(fragment.requireContext())

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> onCancel()
                    else -> onError(errString.toString())
                }
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Usar contraseña")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        BiometricPrompt(fragment, executor, callback).authenticate(promptInfo)
    }
}
