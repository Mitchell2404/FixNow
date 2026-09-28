package com.fixnow.app.presentation.main

import androidx.lifecycle.ViewModel
import com.fixnow.app.domain.usecase.auth.GetCurrentUserUseCase
import com.fixnow.app.domain.usecase.biometric.IsBiometricEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    isBiometricEnabled: IsBiometricEnabledUseCase
) : ViewModel() {

    /**
     * HU04 (sesión persistente) + HU06 (huella):
     * - sin sesión → pantalla de login
     * - con sesión y sin huella activada → directo a Home
     * - con sesión y huella activada → login, donde se pide la huella
     */
    val startAtHome: Boolean = getCurrentUser() != null && !isBiometricEnabled()
}
