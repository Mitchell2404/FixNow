package com.fixnow.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixnow.app.domain.model.User
import com.fixnow.app.domain.usecase.auth.GetCurrentUserUseCase
import com.fixnow.app.domain.usecase.biometric.IsBiometricEnabledUseCase
import com.fixnow.app.domain.usecase.biometric.SetBiometricEnabledUseCase
import com.fixnow.app.domain.usecase.profile.GetUserProfileUseCase
import com.fixnow.app.domain.usecase.profile.UpdateProfileUseCase
import com.fixnow.app.domain.usecase.profile.SaveProfilePhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val email: String = "",
    val biometricEnabled: Boolean = false
)

sealed class ProfileEvent {
    data class ShowMessage(val message: String) : ProfileEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getUserProfile: GetUserProfileUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val saveProfilePhoto: SaveProfilePhotoUseCase,
    private val setBiometricEnabled: SetBiometricEnabledUseCase,
    private val isBiometricEnabled: IsBiometricEnabledUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val uid: String? get() = getCurrentUser()?.uid

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val authUser = getCurrentUser() ?: return
        viewModelScope.launch {
            getUserProfile(authUser.uid)
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = user,
                            email = authUser.email,
                            biometricEnabled = isBiometricEnabled()
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, email = authUser.email) }
                    _events.send(ProfileEvent.ShowMessage(error.message ?: "No se pudo cargar el perfil"))
                }
        }
    }

    /** HU05: guarda nombre y preferencia de notificaciones. */
    fun saveProfile(nombre: String, notificacionesActivas: Boolean) {
        val currentUid = uid ?: return
        if (nombre.length < MIN_NAME_LENGTH) {
            _events.trySend(ProfileEvent.ShowMessage("Ingresa tu nombre"))
            return
        }
        viewModelScope.launch {
            updateProfile(currentUid, nombre, notificacionesActivas)
                .onSuccess { _events.send(ProfileEvent.ShowMessage("Perfil actualizado")) }
                .onFailure { _events.send(ProfileEvent.ShowMessage(it.message ?: "No se pudo guardar el perfil")) }
        }
    }

    /** HU05: guarda la foto tomada con la cámara (reducida y en Base64). [imageUri] es la Uri del archivo, como texto. */
    fun savePhoto(imageUri: String) {
        val currentUid = uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            saveProfilePhoto(currentUid, imageUri)
                .onSuccess { fotoBase64 ->
                    _uiState.update { state ->
                        state.copy(isLoading = false, user = state.user?.copy(fotoBase64 = fotoBase64))
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(ProfileEvent.ShowMessage(error.message ?: "No se pudo guardar la foto"))
                }
        }
    }

    /** HU06: activar/desactivar el ingreso con huella. */
    fun onBiometricToggled(enabled: Boolean) {
        val currentUid = uid ?: return
        _uiState.update { it.copy(biometricEnabled = enabled) }
        viewModelScope.launch { setBiometricEnabled(currentUid, enabled) }
    }

    private companion object {
        const val MIN_NAME_LENGTH = 2
    }
}
