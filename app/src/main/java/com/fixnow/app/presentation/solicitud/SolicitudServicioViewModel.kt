package com.fixnow.app.presentation.solicitud

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.fixnow.app.domain.model.CategoriaServicio
import com.fixnow.app.domain.model.UrgenciaServicio
import com.fixnow.app.domain.usecase.solicitud.CalcularPrecioSugeridoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.fixnow.app.data.local.ImageEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

data class SolicitudServicioUiState(
    val descripcion: String = "",
    val categoria: CategoriaServicio? = null,
    val urgencia: UrgenciaServicio = UrgenciaServicio.NORMAL,
    val precioSugerido: Double? = null,
    val fotoBase64: String = "",
    val procesandoFoto: Boolean = false,
    val errorFoto: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val direccion: String = "",
    val ubicacionAproximada: Boolean = false,
    val obteniendoUbicacion: Boolean = false,
    val errorUbicacion: String? = null
)

@HiltViewModel
class SolicitudServicioViewModel @Inject constructor(
    private val calcularPrecio: CalcularPrecioSugeridoUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val imageEncoder: ImageEncoder,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(leerEstado())

    val uiState: StateFlow<SolicitudServicioUiState> =
        _uiState.asStateFlow()

    fun cambiarDescripcion(descripcion: String) {
        savedStateHandle["descripcion"] = descripcion
        actualizarEstado()
    }

    fun seleccionarCategoria(categoria: CategoriaServicio) {
        savedStateHandle["categoriaId"] = categoria.name
        actualizarEstado()
    }

    fun seleccionarUrgencia(urgencia: UrgenciaServicio) {
        savedStateHandle["urgencia"] = urgencia.name
        actualizarEstado()
    }

    fun procesarFoto(uri: String) {
        if (_uiState.value.procesandoFoto) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    procesandoFoto = true,
                    errorFoto = null
                )
            }

            try {
                val base64 = withContext(Dispatchers.IO) {
                    imageEncoder.toSquareBase64(
                        uriString = uri,
                        targetSizePx = 512
                    )
                }

                require(base64.length <= 180_000) {
                    "La fotografía es demasiado pesada. Intenta tomar otra."
                }

                _uiState.update {
                    it.copy(
                        fotoBase64 = base64,
                        procesandoFoto = false
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        procesandoFoto = false,
                        errorFoto = error.message
                            ?: "No se pudo preparar la fotografía."
                    )
                }
            }
        }
    }

    fun cambiarDireccion(direccion: String) {
        savedStateHandle["direccion"] = direccion
        actualizarEstado()
    }

    fun mostrarErrorUbicacion(mensaje: String) {
        _uiState.update {
            it.copy(errorUbicacion = mensaje)
        }
    }

    fun obtenerUbicacion() {
        if (_uiState.value.obteniendoUbicacion) return

        val permisoPreciso = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val permisoAproximado = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!permisoPreciso && !permisoAproximado) {
            mostrarErrorUbicacion(
                "Permite el acceso a la ubicación para continuar."
            )
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    obteniendoUbicacion = true,
                    errorUbicacion = null
                )
            }

            val cancelacion = CancellationTokenSource()

            try {
                val solicitud = CurrentLocationRequest.Builder()
                    .setPriority(
                        if (permisoPreciso) {
                            Priority.PRIORITY_HIGH_ACCURACY
                        } else {
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY
                        }
                    )
                    .setMaxUpdateAgeMillis(0L)
                    .setDurationMillis(15_000L)
                    .build()

                val ubicacion = LocationServices
                    .getFusedLocationProviderClient(context)
                    .getCurrentLocation(
                        solicitud,
                        cancelacion.token
                    )
                    .await()

                if (ubicacion == null) {
                    mostrarErrorUbicacion(
                        "No se pudo obtener la ubicación. " +
                                "Comprueba que esté activada e inténtalo nuevamente."
                    )
                } else {
                    savedStateHandle["latitud"] = ubicacion.latitude
                    savedStateHandle["longitud"] = ubicacion.longitude
                    savedStateHandle["ubicacionAproximada"] =
                        !permisoPreciso

                    actualizarEstado()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                android.util.Log.e(
                    "UbicacionSolicitud",
                    "Error al obtener la ubicación",
                    error
                )

                mostrarErrorUbicacion(
                    "No se pudo obtener la ubicación. Revisa los permisos e inténtalo nuevamente."
                )
            } finally {
                cancelacion.cancel()

                _uiState.update {
                    it.copy(obteniendoUbicacion = false)
                }
            }
        }
    }

    private fun actualizarEstado() {
        val formulario = leerEstado()

        _uiState.update { actual ->
            formulario.copy(
                fotoBase64 = actual.fotoBase64,
                procesandoFoto = actual.procesandoFoto,
                errorFoto = actual.errorFoto,
                obteniendoUbicacion = actual.obteniendoUbicacion,
                errorUbicacion = actual.errorUbicacion
            )
        }
    }

    private fun leerEstado(): SolicitudServicioUiState {
        val categoriaId = savedStateHandle.get<String>("categoriaId")

        val categoria = CategoriaServicio.entries.firstOrNull {
            it.name == categoriaId
        }

        val urgenciaGuardada = savedStateHandle.get<String>("urgencia")

        val urgencia = UrgenciaServicio.entries.firstOrNull {
            it.name == urgenciaGuardada
        } ?: UrgenciaServicio.NORMAL

        val precio = categoria?.let {
            calcularPrecio(it, urgencia)
        }

        return SolicitudServicioUiState(
            descripcion = savedStateHandle.get<String>("descripcion") ?: "",
            categoria = categoria,
            urgencia = urgencia,
            precioSugerido = precio,
            latitud = savedStateHandle.get<Double>("latitud"),
            longitud = savedStateHandle.get<Double>("longitud"),
            direccion = savedStateHandle.get<String>("direccion") ?: "",
            ubicacionAproximada =
                savedStateHandle.get<Boolean>("ubicacionAproximada") ?: false
        )
    }
}