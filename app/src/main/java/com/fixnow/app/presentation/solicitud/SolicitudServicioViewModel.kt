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

data class SolicitudServicioUiState(
    val descripcion: String = "",
    val categoria: CategoriaServicio? = null,
    val urgencia: UrgenciaServicio = UrgenciaServicio.NORMAL,
    val precioSugerido: Double? = null
)

@HiltViewModel
class SolicitudServicioViewModel @Inject constructor(
    private val calcularPrecio: CalcularPrecioSugeridoUseCase,
    private val savedStateHandle: SavedStateHandle
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

    private fun actualizarEstado() {
        _uiState.value = leerEstado()
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