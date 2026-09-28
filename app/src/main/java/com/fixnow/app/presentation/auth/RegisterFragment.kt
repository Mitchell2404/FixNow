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
import com.fixnow.app.databinding.FragmentRegisterBinding
import dagger.hilt.android.AndroidEntryPoint

/** HU04: registro con correo y contraseña. Al crear la cuenta se envía el correo de verificación. */
@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonRegister.setOnClickListener {
            viewModel.register(
                email = binding.inputEmail.text.toString().trim(),
                password = binding.inputPassword.text.toString(),
                confirmPassword = binding.inputConfirmPassword.text.toString()
            )
        }
        binding.textGoToLogin.setOnClickListener { findNavController().popBackStack() }

        collectWhenStarted(viewModel.uiState) { state ->
            binding.progressRegister.setVisible(state.isLoading)
            binding.buttonRegister.isEnabled = !state.isLoading
        }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                RegisterEvent.NavigateToHome -> findNavController().navigate(R.id.action_register_to_home)
                is RegisterEvent.ShowMessage -> showSnackbar(event.message)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
