package com.fixnow.app.presentation.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

object BiometricAuthManager {

    fun isBiometricAvailable(context: Context): Boolean =
        BiometricManager.from(context)
            .canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            ) == BiometricManager.BIOMETRIC_SUCCESS

    fun showBiometricPrompt(
        fragment: Fragment,
        title: String = "Inicio de sesión biométrico",
        subtitle: String = "Usa tu huella dactilar para ingresar a FixNow",
        onSuccess: () -> Unit,
        onCancel: () -> Unit = {},
        onError: (String) -> Unit
    ) {

        val executor =
            ContextCompat.getMainExecutor(
                fragment.requireContext()
            )

        val callback =
            createCallback(
                onSuccess = onSuccess,
                onCancel = onCancel,
                onError = onError
            )

        val biometricPrompt =
            BiometricPrompt(
                fragment,
                executor,
                callback
            )

        biometricPrompt.authenticate(
            createPromptInfo(
                title = title,
                subtitle = subtitle
            )
        )
    }


    /*
     * Nuevo método.
     */
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
            createCallback(
                onSuccess = onSuccess,
                onCancel = onCancel,
                onError = onError
            )

        val biometricPrompt =
            BiometricPrompt(
                activity,
                executor,
                callback
            )

        biometricPrompt.authenticate(
            createPromptInfo(
                title = title,
                subtitle = subtitle
            )
        )
    }


    private fun createCallback(
        onSuccess: () -> Unit,
        onCancel: () -> Unit,
        onError: (String) -> Unit
    ): BiometricPrompt.AuthenticationCallback {

        return object :
            BiometricPrompt.AuthenticationCallback() {

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
    }


    private fun createPromptInfo(
        title: String,
        subtitle: String
    ): BiometricPrompt.PromptInfo {

        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(
                "Usar contraseña"
            )
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
            .build()
    }
}