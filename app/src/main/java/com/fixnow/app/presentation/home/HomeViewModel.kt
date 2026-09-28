package com.fixnow.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixnow.app.domain.usecase.auth.GetCurrentUserUseCase
import com.fixnow.app.domain.usecase.auth.LogoutUseCase
import com.fixnow.app.domain.usecase.auth.RefreshCurrentUserUseCase
import com.fixnow.app.domain.usecase.auth.ResendVerificationEmailUseCase
import com.fixnow.app.domain.usecase.profile.GetUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val displayName: String = "",
    val emailVerified: Boolean = true
)

sealed class HomeEvent {
    data class ShowMessage(val message: String) : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val refreshCurrentUser: RefreshCurrentUserUseCase,
    private val getUserProfile: GetUserProfileUseCase,
    private val resendVerificationEmail: ResendVerificationEmailUseCase,
    private val logout: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Se llama cada vez que Home se muestra: así, si el usuario verificó su correo, el aviso desaparece. */
    fun refresh() {
        viewModelScope.launch {
            val user = refreshCurrentUser() ?: getCurrentUser() ?: return@launch
            val nombre = getUserProfile(user.uid).getOrNull()?.nombre.orEmpty()
            _uiState.update {
                it.copy(
                    displayName = nombre.ifBlank { user.email },
                    emailVerified = user.isEmailVerified
                )
            }
        }
    }

    fun resendVerification() {
        viewModelScope.launch {
            resendVerificationEmail()
                .onSuccess { _events.send(HomeEvent.ShowMessage("Te enviamos un nuevo correo de verificación")) }
                .onFailure { _events.send(HomeEvent.ShowMessage(it.message ?: "No se pudo enviar el correo")) }
        }
    }

    fun signOut() = logout()
}
