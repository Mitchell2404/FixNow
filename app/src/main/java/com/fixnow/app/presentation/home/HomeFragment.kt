package com.fixnow.app.presentation.home

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
class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()

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

                    HomeScreen(
                        displayName = state.displayName,
                        emailVerified = state.emailVerified,

                        onProfile = {
                            findNavController().navigate(
                                R.id.action_home_to_profile
                            )
                        },

                        onDesignSystem = {
                            findNavController().navigate(
                                R.id.action_home_to_design_system
                            )
                        },

                        onResendVerification = {
                            viewModel.resendVerification()
                        },

                        onLogout = {
                            viewModel.signOut()

                            findNavController().navigate(
                                R.id.action_home_to_login
                            )
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

                is HomeEvent.ShowMessage -> {
                    showSnackbar(
                        event.message
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        viewModel.refresh()
    }
}