package com.fixnow.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixnow.app.core.util.isValidEmail
import com.fixnow.app.domain.model.AuthResult
import com.fixnow.app.domain.usecase.auth.RegisterUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(val isLoading: Boolean = false)

sealed class RegisterEvent {
    object NavigateToHome : RegisterEvent()
    data class ShowMessage(val message: String) : RegisterEvent()
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUser: RegisterUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _events = Channel<RegisterEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun register(email: String, password: String, confirmPassword: String) {
        val error = when {
            !email.isValidEmail() -> "Ingresa un correo válido"
            password.length < MIN_PASSWORD_LENGTH -> "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres"
            password != confirmPassword -> "Las contraseñas no coinciden"
            else -> null
        }
        if (error != null) {
            _events.trySend(RegisterEvent.ShowMessage(error))
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = registerUser(email, password)) {
                is AuthResult.Success -> _events.send(RegisterEvent.NavigateToHome)
                is AuthResult.Error -> _events.send(RegisterEvent.ShowMessage(result.message))
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
