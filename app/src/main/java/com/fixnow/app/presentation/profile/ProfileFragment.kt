package com.fixnow.app.presentation.profile

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fixnow.app.R
import com.fixnow.app.core.util.collectWhenStarted
import com.fixnow.app.core.util.showSnackbar
import com.fixnow.app.presentation.biometric.BiometricAuthManager
import com.fixnow.app.presentation.theme.FixNowTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    private var pendingPhotoUri: Uri? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                launchCamera()

            } else {

                showSnackbar(
                    "Se necesita el permiso de cámara para tomar la foto"
                )
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->

            if (success) {

                pendingPhotoUri?.let { uri ->

                    viewModel.savePhoto(
                        uri.toString()
                    )
                }
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            // Por ahora mantenemos el mismo comportamiento
            // que tenía la versión XML.
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val biometricSupported =
            BiometricAuthManager.isBiometricAvailable(
                requireContext()
            )

        return ComposeView(requireContext()).apply {

            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )

            setContent {

                FixNowTheme {

                    val state by viewModel.uiState
                        .collectAsStateWithLifecycle()

                    ProfileScreen(
                        user = state.user,
                        email = state.email,
                        isLoading = state.isLoading,
                        biometricEnabled = state.biometricEnabled,
                        biometricSupported = biometricSupported,

                        onTakePhoto = {
                            requestCamera()
                        },

                        onSaveProfile = { name, notifications ->

                            viewModel.saveProfile(
                                nombre = name,
                                notificacionesActivas = notifications
                            )
                        },

                        onNotificationsChanged = { enabled ->

                            if (
                                enabled &&
                                Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.TIRAMISU
                            ) {

                                notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            }
                        },

                        onBiometricChanged = { enabled ->

                            handleBiometricChange(
                                enabled
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

        collectWhenStarted(
            viewModel.events
        ) { event ->

            when (event) {

                is ProfileEvent.ShowMessage -> {
                    showSnackbar(
                        event.message
                    )
                }
            }
        }
    }

    private fun requestCamera() {

        cameraPermissionLauncher.launch(
            Manifest.permission.CAMERA
        )
    }

    private fun launchCamera() {

        runCatching {

            val photoFile = File.createTempFile(
                "profile_",
                ".jpg",
                requireContext().cacheDir
            )

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )

            pendingPhotoUri = uri

            takePictureLauncher.launch(uri)

        }.onFailure {

            showSnackbar(
                "No se pudo abrir la cámara"
            )
        }
    }

    private fun handleBiometricChange(
        enabled: Boolean
    ) {

        if (!enabled) {

            viewModel.onBiometricToggled(false)
            return
        }

        BiometricAuthManager.showBiometricPrompt(
            fragment = this,
            title = getString(
                R.string.profile_biometric_confirm_title
            ),
            subtitle = getString(
                R.string.profile_biometric_confirm_subtitle
            ),

            onSuccess = {
                viewModel.onBiometricToggled(true)
            },

            onCancel = {

            },

            onError = { message ->
                showSnackbar(message)
            }
        )
    }
}