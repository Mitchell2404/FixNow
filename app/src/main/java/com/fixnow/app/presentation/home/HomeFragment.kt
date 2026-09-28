package com.fixnow.app.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.fixnow.app.R
import com.fixnow.app.core.util.collectWhenStarted
import com.fixnow.app.core.util.setVisible
import com.fixnow.app.core.util.showSnackbar
import com.fixnow.app.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Pantalla de inicio provisional. Aquí irá el flujo de solicitudes de servicio (HU07 en adelante).
 * Por ahora sirve de punto de partida para probar el perfil y el sistema de diseño.
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonProfile.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_profile)
        }
        binding.buttonDesignSystem.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_design_system)
        }
        binding.buttonResendVerification.setOnClickListener { viewModel.resendVerification() }
        binding.buttonLogout.setOnClickListener {
            viewModel.signOut()
            findNavController().navigate(R.id.action_home_to_login)
        }

        collectWhenStarted(viewModel.uiState) { state ->
            binding.textGreeting.text = if (state.displayName.isBlank()) {
                getString(R.string.home_greeting_no_name)
            } else {
                getString(R.string.home_greeting, state.displayName)
            }
            binding.cardVerifyEmail.setVisible(!state.emailVerified)
        }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                is HomeEvent.ShowMessage -> showSnackbar(event.message)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
