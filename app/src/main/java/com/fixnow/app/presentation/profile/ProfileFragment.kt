package com.fixnow.app.presentation.profile

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.fixnow.app.R
import com.fixnow.app.core.util.collectWhenStarted
import com.fixnow.app.core.util.setVisible
import com.fixnow.app.core.util.showSnackbar
import com.fixnow.app.databinding.FragmentProfileBinding
import com.fixnow.app.domain.model.User
import com.fixnow.app.presentation.biometric.BiometricAuthManager
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

/**
 * HU05: perfil (nombre, foto con la cámara, notificaciones).
 * HU06: interruptor para activar/desactivar el ingreso con huella.
 */
@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    private var pendingPhotoUri: Uri? = null
    private var formBound = false
    private var shownPhoto = ""

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera() else showSnackbar("Se necesita el permiso de cámara para tomar la foto")
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingPhotoUri?.let { viewModel.savePhoto(it.toString()) }
    }

    // Android 13+ pide permiso para mostrar notificaciones. Si lo rechaza, no pasa nada más aquí.
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        formBound = false
        shownPhoto = ""

        binding.buttonTakePhoto.setOnClickListener {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        binding.buttonSaveProfile.setOnClickListener {
            viewModel.saveProfile(
                nombre = binding.inputName.text.toString().trim(),
                notificacionesActivas = binding.switchNotifications.isChecked
            )
        }

        // "isPressed" distingue un toque del usuario de un cambio hecho por el código al cargar.
        binding.switchNotifications.setOnCheckedChangeListener { button, checked ->
            if (checked && button.isPressed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setupBiometricSwitch()

        collectWhenStarted(viewModel.uiState) { state ->
            binding.progressProfile.setVisible(state.isLoading)
            binding.textEmail.text = state.email
            state.user?.let { bindUser(it, state.biometricEnabled) }
        }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                is ProfileEvent.ShowMessage -> showSnackbar(event.message)
            }
        }
    }

    private fun setupBiometricSwitch() {
        val supported = BiometricAuthManager.isBiometricAvailable(requireContext())
        binding.switchBiometric.isEnabled = supported
        if (!supported) binding.switchBiometric.setText(R.string.profile_biometric_unavailable)

        binding.switchBiometric.setOnCheckedChangeListener { button, checked ->
            if (!button.isPressed) return@setOnCheckedChangeListener
            if (checked) {
                // Se confirma con la huella antes de activarlo, para asegurar que hay una registrada.
                BiometricAuthManager.showBiometricPrompt(
                    fragment = this,
                    title = getString(R.string.profile_biometric_confirm_title),
                    subtitle = getString(R.string.profile_biometric_confirm_subtitle),
                    onSuccess = { viewModel.onBiometricToggled(true) },
                    onCancel = { binding.switchBiometric.isChecked = false },
                    onError = { message ->
                        binding.switchBiometric.isChecked = false
                        showSnackbar(message)
                    }
                )
            } else {
                viewModel.onBiometricToggled(false)
            }
        }
    }

    private fun bindUser(user: User, biometricEnabled: Boolean) {
        // El formulario se rellena una sola vez, para no pisar lo que el usuario esté escribiendo.
        if (!formBound) {
            formBound = true
            binding.inputName.setText(user.nombre)
            binding.switchNotifications.isChecked = user.notificacionesActivas
            binding.switchBiometric.isChecked = biometricEnabled &&
                BiometricAuthManager.isBiometricAvailable(requireContext())
        }
        // Solo se vuelve a decodificar la foto si cambió.
        if (user.fotoBase64.isNotBlank() && user.fotoBase64 != shownPhoto) {
            shownPhoto = user.fotoBase64
            val bytes = Base64.decode(user.fotoBase64, Base64.DEFAULT)
            Glide.with(this).load(bytes).circleCrop().into(binding.imageProfilePhoto)
        }
    }

    private fun launchCamera() {
        val photoFile = File.createTempFile("profile_", ".jpg", requireContext().cacheDir)
        val uri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", photoFile
        )
        pendingPhotoUri = uri
        takePictureLauncher.launch(uri)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
