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
import com.fixnow.app.presentation.theme.FixNowTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return ComposeView(requireContext()).apply {

            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )

            setContent {

                FixNowTheme {

                    val state by viewModel.uiState
                        .collectAsStateWithLifecycle()

                    RegisterScreen(
                        isLoading = state.isLoading,

                        onRegister = { email, password, confirmPassword ->

                            viewModel.register(
                                email = email,
                                password = password,
                                confirmPassword = confirmPassword
                            )
                        },

                        onGoToLogin = {
                            findNavController().popBackStack()
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
        super.onViewCreated(
            view,
            savedInstanceState
        )

        collectWhenStarted(viewModel.events) { event ->

            when (event) {

                RegisterEvent.NavigateToHome -> {
                    findNavController().navigate(
                        R.id.action_register_to_home
                    )
                }

                is RegisterEvent.ShowMessage -> {
                    showSnackbar(
                        event.message
                    )
                }
            }
        }
    }
}