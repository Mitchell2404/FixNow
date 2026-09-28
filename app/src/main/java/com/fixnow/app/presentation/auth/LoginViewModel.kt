package com.fixnow.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixnow.app.core.util.isValidEmail
import com.fixnow.app.domain.model.AuthResult
import com.fixnow.app.domain.usecase.auth.GetCurrentUserUseCase
import com.fixnow.app.domain.usecase.auth.LoginUserUseCase
import com.fixnow.app.domain.usecase.auth.SendPasswordResetUseCase
import com.fixnow.app.domain.usecase.biometric.IsBiometricEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Lo que la pantalla "es" en cada momento (se vuelve a pintar igual tras girar el teléfono). */
data class LoginUiState(val isLoading: Boolean = false)

/** Cosas que pasan una sola vez: navegar o mostrar un mensaje. */
sealed class LoginEvent {
    object NavigateToHome : LoginEvent()
    data class ShowMessage(val message: String) : LoginEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUser: LoginUserUseCase,
    private val sendPasswordReset: SendPasswordResetUseCase,
    getCurrentUser: GetCurrentUserUseCase,
    isBiometricEnabled: IsBiometricEnabledUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** HU06: hay una sesión guardada y el usuario activó la huella en este dispositivo. */
    val biometricShortcutEnabled: Boolean = getCurrentUser() != null && isBiometricEnabled()

    fun login(email: String, password: String) {
        if (!email.isValidEmail()) {
            _events.trySend(LoginEvent.ShowMessage("Ingresa un correo válido"))
            return
        }
        if (password.isBlank()) {
            _events.trySend(LoginEvent.ShowMessage("Ingresa tu contraseña"))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = loginUser(email, password)) {
                is AuthResult.Success -> _events.send(LoginEvent.NavigateToHome)
                is AuthResult.Error -> _events.send(LoginEvent.ShowMessage(result.message))
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onForgotPassword(email: String) {
        if (!email.isValidEmail()) {
            _events.trySend(LoginEvent.ShowMessage("Escribe tu correo arriba para recuperar la contraseña"))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            sendPasswordReset(email)
                .onSuccess {
                    _events.send(LoginEvent.ShowMessage("Te enviamos un correo para restablecer tu contraseña"))
                }
                .onFailure {
                    _events.send(LoginEvent.ShowMessage(it.message ?: "No se pudo enviar el correo"))
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /** HU06: la huella confirmó la identidad; la sesión de Firebase ya existía. */
    fun onBiometricSuccess() {
        _events.trySend(LoginEvent.NavigateToHome)
    }
}
