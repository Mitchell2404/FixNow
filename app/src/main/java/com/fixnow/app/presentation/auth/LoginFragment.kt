package com.fixnow.app.presentation.auth

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
import com.fixnow.app.databinding.FragmentLoginBinding
import com.fixnow.app.presentation.biometric.BiometricAuthManager
import dagger.hilt.android.AndroidEntryPoint

/** HU04 (login por correo y contraseña) + HU06 (ingreso con huella). Sin lógica de negocio aquí. */
@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonLogin.setOnClickListener {
            viewModel.login(
                email = binding.inputEmail.text.toString().trim(),
                password = binding.inputPassword.text.toString()
            )
        }
        binding.textForgotPassword.setOnClickListener {
            viewModel.onForgotPassword(binding.inputEmail.text.toString().trim())
        }
        binding.textGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        setupBiometricShortcut(autoPrompt = savedInstanceState == null)

        collectWhenStarted(viewModel.uiState) { state ->
            binding.progressLogin.setVisible(state.isLoading)
            binding.buttonLogin.isEnabled = !state.isLoading
        }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                LoginEvent.NavigateToHome -> findNavController().navigate(R.id.action_login_to_home)
                is LoginEvent.ShowMessage -> showSnackbar(event.message)
            }
        }
    }

    private fun setupBiometricShortcut(autoPrompt: Boolean) {
        val available = viewModel.biometricShortcutEnabled &&
            BiometricAuthManager.isBiometricAvailable(requireContext())

        binding.buttonBiometricLogin.setVisible(available)
        if (!available) return

        binding.buttonBiometricLogin.setOnClickListener { promptBiometric() }
        // Al abrir la app con la huella activada, se pide de una vez.
        if (autoPrompt) binding.root.post { if (isAdded) promptBiometric() }
    }

    private fun promptBiometric() {
        BiometricAuthManager.showBiometricPrompt(
            fragment = this,
            onSuccess = viewModel::onBiometricSuccess,
            onError = { message -> showSnackbar(message) }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
