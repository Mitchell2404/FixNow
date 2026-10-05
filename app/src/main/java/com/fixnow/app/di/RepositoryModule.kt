package com.fixnow.app.di

import com.fixnow.app.data.local.BiometricSettingsRepositoryImpl
import com.fixnow.app.data.repository.AuthRepositoryImpl
import com.fixnow.app.data.repository.ProfileRepositoryImpl
import com.fixnow.app.domain.repository.AuthRepository
import com.fixnow.app.domain.repository.BiometricSettingsRepository
import com.fixnow.app.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.fixnow.app.data.repository.SolicitudRepositoryImpl
import com.fixnow.app.domain.repository.SolicitudRepository

/**
 * HU01: le dice a Hilt qué clase concreta usar cuando alguien pide una interfaz.
 * Así los casos de uso dependen de interfaces (domain) y no de Firebase (data).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    abstract fun bindBiometricSettingsRepository(impl: BiometricSettingsRepositoryImpl): BiometricSettingsRepository

    @Binds
    abstract fun bindSolicitudRepository(
        impl: SolicitudRepositoryImpl
    ): SolicitudRepository
}
