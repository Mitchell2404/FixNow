package com.fixnow.app.presentation.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.fixnow.app.R
import com.fixnow.app.core.util.collectWhenStarted
import com.fixnow.app.core.util.showSnackbar
import com.fixnow.app.presentation.biometric.BiometricAuthManager
import com.fixnow.app.presentation.theme.FixNowTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val biometricAvailable = isBiometricShortcutAvailable()

        return ComposeView(requireContext()).apply {

            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )

            setContent {

                FixNowTheme {

                    val state by viewModel.uiState
                        .collectAsStateWithLifecycle()

                    LoginScreen(
                        isLoading = state.isLoading,
                        showBiometricLogin = biometricAvailable,

                        onLogin = { email, password ->
                            viewModel.login(
                                email = email,
                                password = password
                            )
                        },

                        onForgotPassword = { email ->
                            viewModel.onForgotPassword(email)
                        },

                        onRegister = {
                            findNavController().navigate(
                                R.id.action_login_to_register
                            )
                        },

                        onBiometricLogin = {
                            promptBiometric()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        collectWhenStarted(viewModel.events) { event ->

            when (event) {

                LoginEvent.NavigateToHome -> {
                    findNavController().navigate(
                        R.id.action_login_to_home
                    )
                }

                is LoginEvent.ShowMessage -> {
                    showSnackbar(event.message)
                }
            }
        }

        // Si el usuario ya tiene biometría activada,
        // mostramos automáticamente el diálogo al abrir.
        if (
            savedInstanceState == null &&
            isBiometricShortcutAvailable()
        ) {
            view.post {
                if (isAdded) {
                    promptBiometric()
                }
            }
        }
    }

    private fun isBiometricShortcutAvailable(): Boolean {

        return viewModel.biometricShortcutEnabled &&
                BiometricAuthManager.isBiometricAvailable(
                    requireContext()
                )
    }

    private fun promptBiometric() {

        BiometricAuthManager.showBiometricPrompt(
            fragment = this,
            onSuccess = {
                viewModel.onBiometricSuccess()
            },
            onError = { message ->
                showSnackbar(message)
            }
        )
    }
}