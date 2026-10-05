package com.fixnow.app.presentation.solicitud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixnow.app.domain.model.SolicitudServicio
import com.fixnow.app.domain.usecase.solicitud.ObtenerMisSolicitudesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.fixnow.app.domain.model.EstadoSolicitud
import com.fixnow.app.domain.usecase.solicitud.CancelarSolicitudUseCase

data class MisSolicitudesUiState(
    val cargando: Boolean = false,
    val solicitudes: List<SolicitudServicio> = emptyList(),
    val error: String? = null,
    val cancelandoId: String? = null,
    val mensaje: String? = null
)

@HiltViewModel
class MisSolicitudesViewModel @Inject constructor(
    private val obtenerMisSolicitudes: ObtenerMisSolicitudesUseCase,
    private val cancelarSolicitudUseCase: CancelarSolicitudUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MisSolicitudesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        cargarSolicitudes()
    }

    fun cargarSolicitudes() {
        if (
            _uiState.value.cargando ||
            _uiState.value.cancelandoId != null
        ) return

        _uiState.update {
            it.copy(
                cargando = true,
                error = null,
                mensaje = null
            )
        }

        viewModelScope.launch {
            try {
                obtenerMisSolicitudes().fold(
                    onSuccess = { solicitudes ->
                        _uiState.update {
                            it.copy(solicitudes = solicitudes)
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                error = error.message
                                    ?: "No se pudieron cargar las solicitudes."
                            )
                        }
                    }
                )
            } finally {
                _uiState.update {
                    it.copy(cargando = false)
                }
            }
        }
    }

    fun cancelarSolicitud(solicitudId: String) {
        val estado = _uiState.value

        if (
            estado.cargando ||
            estado.cancelandoId != null
        ) return

        _uiState.update {
            it.copy(
                cancelandoId = solicitudId,
                error = null,
                mensaje = null
            )
        }

        viewModelScope.launch {
            try {
                cancelarSolicitudUseCase(solicitudId).fold(
                    onSuccess = {
                        // Actualizar la tarjeta después de confirmar
                        // el cambio en Firestore.
                        _uiState.update { actual ->
                            actual.copy(
                                solicitudes = actual.solicitudes.map {
                                    if (it.id == solicitudId) {
                                        it.copy(
                                            estado = EstadoSolicitud
                                                .CANCELADA.name
                                        )
                                    } else {
                                        it
                                    }
                                },
                                mensaje = "Solicitud cancelada correctamente."
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                error = error.message
                                    ?: "No se pudo cancelar. Actualiza el listado."
                            )
                        }
                    }
                )
            } finally {
                _uiState.update {
                    it.copy(cancelandoId = null)
                }
            }
        }
    }
}