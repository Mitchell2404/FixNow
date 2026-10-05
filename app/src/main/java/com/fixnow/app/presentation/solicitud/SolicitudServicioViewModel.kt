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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SolicitudServicioUiState(
    val descripcion: String = "",
    val categoria: CategoriaServicio? = null,
    val urgencia: UrgenciaServicio = UrgenciaServicio.NORMAL,
    val precioSugerido: Double? = null,
    val fotoBase64: String = "",
    val procesandoFoto: Boolean = false,
    val errorFoto: String? = null
)

@HiltViewModel
class SolicitudServicioViewModel @Inject constructor(
    private val calcularPrecio: CalcularPrecioSugeridoUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val imageEncoder: ImageEncoder
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

    private fun actualizarEstado() {
        val formulario = leerEstado()

        _uiState.update { actual ->
            formulario.copy(
                fotoBase64 = actual.fotoBase64,
                procesandoFoto = actual.procesandoFoto,
                errorFoto = actual.errorFoto
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
            precioSugerido = precio
        )
    }
}