package com.fixnow.app.presentation.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthManager {

    fun isBiometricAvailable(context: Context): Boolean =
        BiometricManager.from(context)
            .canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            ) == BiometricManager.BIOMETRIC_SUCCESS

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Inicio de sesión biométrico",
        subtitle: String = "Usa tu huella dactilar para ingresar a FixNow",
        onSuccess: () -> Unit,
        onCancel: () -> Unit = {},
        onError: (String) -> Unit
    ) {

        val executor =
            ContextCompat.getMainExecutor(activity)

        val callback =
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)

                    onSuccess()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(
                        errorCode,
                        errString
                    )

                    when (errorCode) {

                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> {
                            onCancel()
                        }

                        else -> {
                            onError(
                                errString.toString()
                            )
                        }
                    }
                }
            }

        val biometricPrompt =
            BiometricPrompt(
                activity,
                executor,
                callback
            )

        val promptInfo =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(
                    "Usar contraseña"
                )
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                )
                .build()

        biometricPrompt.authenticate(
            promptInfo
        )
    }
}