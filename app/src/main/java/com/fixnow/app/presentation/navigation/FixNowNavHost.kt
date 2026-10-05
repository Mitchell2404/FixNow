package com.fixnow.app.presentation.navigation

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.fixnow.app.presentation.auth.LoginEvent
import com.fixnow.app.presentation.auth.LoginScreen
import com.fixnow.app.presentation.auth.LoginViewModel
import com.fixnow.app.presentation.auth.RegisterEvent
import com.fixnow.app.presentation.auth.RegisterScreen
import com.fixnow.app.presentation.auth.RegisterViewModel
import com.fixnow.app.presentation.biometric.BiometricAuthManager
import com.fixnow.app.presentation.designsystem.DesignSystemScreen
import com.fixnow.app.presentation.home.HomeEvent
import com.fixnow.app.presentation.home.HomeScreen
import com.fixnow.app.presentation.home.HomeViewModel
import com.fixnow.app.presentation.profile.ProfileEvent
import com.fixnow.app.presentation.profile.ProfileScreen
import com.fixnow.app.presentation.profile.ProfileViewModel
import kotlinx.coroutines.launch
import java.io.File
import com.fixnow.app.presentation.solicitud.SolicitudServicioScreen
import com.fixnow.app.presentation.solicitud.SolicitudServicioViewModel

@Composable
fun FixNowNavHost(
    navController: NavHostController,
    startAtHome: Boolean,
    activity: FragmentActivity,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {

    NavHost(
        navController = navController,
        startDestination = if (startAtHome) {
            Routes.HOME
        } else {
            Routes.LOGIN
        },
        modifier = modifier
    ) {

        // LOGIN
        composable(Routes.LOGIN) {

            LoginRoute(
                navController = navController,
                activity = activity,
                snackbarHostState = snackbarHostState
            )
        }


        // REGISTRO
        composable(Routes.REGISTER) {

            RegisterRoute(
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }


        // HOME
        composable(Routes.HOME) {

            HomeRoute(
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }


        // PERFIL
        composable(Routes.PROFILE) {

            ProfileRoute(
                activity = activity,
                snackbarHostState = snackbarHostState
            )
        }


        // SISTEMA DE DISEÑO
        composable(Routes.DESIGN_SYSTEM) {

            DesignSystemScreen()
        }

        // SISTEMA DE SOLICITUD
        composable(Routes.NUEVA_SOLICITUD) {
            val viewModel: SolicitudServicioViewModel = hiltViewModel()

            val state by viewModel.uiState.collectAsStateWithLifecycle()

            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            var fotoPendienteUri by rememberSaveable {
                mutableStateOf<String?>(null)
            }

            // Recibe el resultado de la cámara.
            val camaraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicture()
            ) { guardada ->
                val uri = fotoPendienteUri
                fotoPendienteUri = null

                if (guardada && uri != null) {
                    viewModel.procesarFoto(uri)
                }
            }

            // Solicita permiso y abre la cámara.
            val permisoLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { concedido ->
                if (concedido) {
                    try {
                        val archivo = File.createTempFile(
                            "solicitud_",
                            ".jpg",
                            context.cacheDir
                        )

                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            archivo
                        )

                        fotoPendienteUri = uri.toString()
                        camaraLauncher.launch(uri)
                    } catch (error: Exception) {
                        fotoPendienteUri = null

                        android.util.Log.e(
                            "FotoSolicitud",
                            "Error al abrir la cámara",
                            error
                        )

                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "${error.javaClass.simpleName}: " +
                                        (error.message ?: "Error sin detalle"),
                                duration = androidx.compose.material3.SnackbarDuration.Long
                            )
                        }
                    }
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Se necesita permiso de cámara. " +
                                    "Puedes habilitarlo en los ajustes de la aplicación."
                        )
                    }
                }
            }

            val permisoUbicacionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permisos ->
                val preciso =
                    permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true

                val aproximado =
                    permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true

                if (preciso || aproximado) {
                    viewModel.obtenerUbicacion()
                } else {
                    viewModel.mostrarErrorUbicacion(
                        "No se concedió el permiso de ubicación. " +
                                "Puedes habilitarlo en los ajustes de la aplicación."
                    )
                }
            }

            SolicitudServicioScreen(
                state = state,
                onDescripcionChange = viewModel::cambiarDescripcion,
                onCategoriaChange = viewModel::seleccionarCategoria,
                onUrgenciaChange = viewModel::seleccionarUrgencia,
                onTomarFoto = {
                    permisoLauncher.launch(
                        Manifest.permission.CAMERA
                    )
                },
                onObtenerUbicacion = {
                    permisoUbicacionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                onDireccionChange = viewModel::cambiarDireccion,
                onEnviarSolicitud = viewModel::enviarSolicitud,
                onVolver = {
                    navController.popBackStack()
                }
            )
        }
    }
}


// =====================================================
// LOGIN
// =====================================================

@Composable
private fun LoginRoute(
    navController: NavHostController,
    activity: FragmentActivity,
    snackbarHostState: SnackbarHostState,
    viewModel: LoginViewModel = hiltViewModel()
) {

    val state by viewModel.uiState
        .collectAsStateWithLifecycle()

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val biometricAvailable =
        viewModel.biometricShortcutEnabled &&
                BiometricAuthManager.isBiometricAvailable(
                    context
                )

    var autoBiometricShown by rememberSaveable {
        mutableStateOf(false)
    }


    // Escuchar eventos del ViewModel
    LaunchedEffect(viewModel) {

        viewModel.events.collect { event ->

            when (event) {

                LoginEvent.NavigateToHome -> {

                    navController.navigate(
                        Routes.HOME
                    ) {

                        popUpTo(Routes.LOGIN) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }


                is LoginEvent.ShowMessage -> {

                    snackbarHostState.showSnackbar(
                        event.message
                    )
                }
            }
        }
    }


    // Mostrar huella automáticamente si ya estaba activada
    LaunchedEffect(biometricAvailable) {

        if (
            biometricAvailable &&
            !autoBiometricShown
        ) {

            autoBiometricShown = true

            BiometricAuthManager.showBiometricPrompt(
                activity = activity,

                onSuccess = {

                    viewModel.onBiometricSuccess()
                },

                onError = { message ->

                    scope.launch {

                        snackbarHostState.showSnackbar(
                            message
                        )
                    }
                }
            )
        }
    }


    LoginScreen(
        isLoading = state.isLoading,

        showBiometricLogin =
            biometricAvailable,

        onLogin = { email, password ->

            viewModel.login(
                email = email,
                password = password
            )
        },

        onForgotPassword = { email ->

            viewModel.onForgotPassword(
                email
            )
        },

        onRegister = {

            navController.navigate(
                Routes.REGISTER
            )
        },

        onBiometricLogin = {

            BiometricAuthManager.showBiometricPrompt(
                activity = activity,

                onSuccess = {

                    viewModel.onBiometricSuccess()
                },

                onError = { message ->

                    scope.launch {

                        snackbarHostState.showSnackbar(
                            message
                        )
                    }
                }
            )
        }
    )
}


// =====================================================
// REGISTRO
// =====================================================

@Composable
private fun RegisterRoute(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    viewModel: RegisterViewModel = hiltViewModel()
) {

    val state by viewModel.uiState
        .collectAsStateWithLifecycle()


    LaunchedEffect(viewModel) {

        viewModel.events.collect { event ->

            when (event) {

                RegisterEvent.NavigateToHome -> {

                    navController.navigate(
                        Routes.HOME
                    ) {

                        popUpTo(Routes.LOGIN) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }


                is RegisterEvent.ShowMessage -> {

                    snackbarHostState.showSnackbar(
                        event.message
                    )
                }
            }
        }
    }


    RegisterScreen(
        isLoading = state.isLoading,

        onRegister = {
                email,
                password,
                confirmPassword ->

            viewModel.register(
                email = email,
                password = password,
                confirmPassword = confirmPassword
            )
        },

        onGoToLogin = {

            navController.popBackStack()
        }
    )
}


// =====================================================
// HOME
// =====================================================

@Composable
private fun HomeRoute(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = hiltViewModel()
) {

    val state by viewModel.uiState
        .collectAsStateWithLifecycle()

    val lifecycleOwner =
        LocalLifecycleOwner.current


    /*
     * Equivale al antiguo HomeFragment.onStart().
     *
     * Cuando Home vuelve a mostrarse se actualizan
     * los datos del usuario y el estado del correo.
     */
    DisposableEffect(lifecycleOwner) {

        val observer =
            LifecycleEventObserver { _, event ->

                if (
                    event ==
                    Lifecycle.Event.ON_START
                ) {

                    viewModel.refresh()
                }
            }


        lifecycleOwner.lifecycle.addObserver(
            observer
        )


        onDispose {

            lifecycleOwner.lifecycle.removeObserver(
                observer
            )
        }
    }


    LaunchedEffect(viewModel) {

        viewModel.events.collect { event ->

            when (event) {

                is HomeEvent.ShowMessage -> {

                    snackbarHostState.showSnackbar(
                        event.message
                    )
                }
            }
        }
    }


    HomeScreen(
        displayName =
            state.displayName,

        emailVerified =
            state.emailVerified,

        onNuevaSolicitud = {
            navController.navigate(Routes.NUEVA_SOLICITUD) {
                launchSingleTop = true
            }
        },

        onProfile = {

            navController.navigate(
                Routes.PROFILE
            )
        },

        onDesignSystem = {

            navController.navigate(
                Routes.DESIGN_SYSTEM
            )
        },

        onResendVerification = {

            viewModel.resendVerification()
        },

        onLogout = {

            viewModel.signOut()

            navController.navigate(
                Routes.LOGIN
            ) {

                popUpTo(Routes.HOME) {
                    inclusive = true
                }

                launchSingleTop = true
            }
        }
    )
}


// =====================================================
// PERFIL
// =====================================================

@Composable
private fun ProfileRoute(
    activity: FragmentActivity,
    snackbarHostState: SnackbarHostState,
    viewModel: ProfileViewModel = hiltViewModel()
) {

    val state by viewModel.uiState
        .collectAsStateWithLifecycle()

    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    val biometricSupported =
        BiometricAuthManager.isBiometricAvailable(
            context
        )


    /*
     * Guardamos String en lugar de Uri directamente
     * para que rememberSaveable pueda conservarlo.
     */
    var pendingPhotoUri by rememberSaveable {
        mutableStateOf<String?>(null)
    }


    // -------------------------------------------------
    // CÁMARA
    // -------------------------------------------------

    val takePictureLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.TakePicture()
        ) { success ->

            if (success) {

                pendingPhotoUri?.let { uri ->

                    viewModel.savePhoto(
                        uri
                    )
                }
            }
        }


    // -------------------------------------------------
    // PERMISO DE CÁMARA
    // -------------------------------------------------

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                runCatching {

                    val photoFile =
                        File.createTempFile(
                            "profile_",
                            ".jpg",
                            context.cacheDir
                        )


                    val uri =
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            photoFile
                        )


                    pendingPhotoUri =
                        uri.toString()


                    takePictureLauncher.launch(
                        uri
                    )

                }.onFailure {

                    scope.launch {

                        snackbarHostState.showSnackbar(
                            "No se pudo abrir la cámara"
                        )
                    }
                }

            } else {

                scope.launch {

                    snackbarHostState.showSnackbar(
                        "Se necesita el permiso de cámara para tomar la foto"
                    )
                }
            }
        }


    // -------------------------------------------------
    // PERMISO DE NOTIFICACIONES
    // -------------------------------------------------

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) {
            /*
             * Por ahora mantenemos el comportamiento
             * que ya tenía la aplicación.
             */
        }


    // -------------------------------------------------
    // EVENTOS DEL VIEWMODEL
    // -------------------------------------------------

    LaunchedEffect(viewModel) {

        viewModel.events.collect { event ->

            when (event) {

                is ProfileEvent.ShowMessage -> {

                    snackbarHostState.showSnackbar(
                        event.message
                    )
                }
            }
        }
    }


    // -------------------------------------------------
    // UI
    // -------------------------------------------------

    ProfileScreen(
        user =
            state.user,

        email =
            state.email,

        isLoading =
            state.isLoading,

        biometricEnabled =
            state.biometricEnabled,

        biometricSupported =
            biometricSupported,


        // Tomar foto
        onTakePhoto = {

            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        },


        // Guardar perfil
        onSaveProfile = {
                name,
                notifications ->

            viewModel.saveProfile(
                nombre = name,
                notificacionesActivas =
                    notifications
            )
        },


        // Cambio en notificaciones
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


        // Activar/desactivar biometría
        onBiometricChanged = { enabled ->

            if (!enabled) {

                viewModel.onBiometricToggled(
                    false
                )

            } else {

                BiometricAuthManager.showBiometricPrompt(
                    activity = activity,

                    title =
                        "Confirmar huella",

                    subtitle =
                        "Confirma tu identidad para activar el acceso biométrico",

                    onSuccess = {

                        viewModel.onBiometricToggled(
                            true
                        )
                    },

                    onCancel = {
                        /*
                         * No hacemos nada.
                         *
                         * El ViewModel continúa en false,
                         * por lo tanto el Switch queda apagado.
                         */
                    },

                    onError = { message ->

                        scope.launch {

                            snackbarHostState.showSnackbar(
                                message
                            )
                        }
                    }
                )
            }
        }
    )
}