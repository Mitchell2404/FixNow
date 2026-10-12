package com.fixnow.app.presentation.tecnico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixnow.app.data.remote.GeocodingApi
import com.fixnow.app.domain.model.ReputacionUsuario
import com.fixnow.app.domain.model.SolicitudServicio
import com.fixnow.app.domain.repository.SolicitudRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

enum class OrdenSolicitudes(val etiqueta: String) {
    DISTANCIA("Más cercanas"),
    URGENCIA("Más urgentes"),
    RECIENTES("Más recientes")
}

enum class VistaSolicitudes {
    LISTA,
    MAPA
}

data class TecnicoUiState(
    val solicitudes: List<SolicitudServicio> = emptyList(),
    val misSolicitudes: List<SolicitudServicio> = emptyList(),
    val reputaciones: Map<String, ReputacionUsuario> = emptyMap(),
    val latitud: Double? = null,
    val longitud: Double? = null,
    val orden: OrdenSolicitudes = OrdenSolicitudes.DISTANCIA,
    val vista: VistaSolicitudes = VistaSolicitudes.LISTA,
    val mostrarMisSolicitudes: Boolean = false,
    val seleccionadaId: String? = null,
    val direccionesGeocodificadas: Map<String, String> = emptyMap(),
    val geocodificandoId: String? = null,
    val cargando: Boolean = true,
    val error: String? = null
)

sealed interface TecnicoEvent {
    data class ShowMessage(val message: String) : TecnicoEvent
}

@HiltViewModel
class SolicitudesTecnicoViewModel @Inject constructor(
    private val repository: SolicitudRepository,
    private val geocodingApi: GeocodingApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(TecnicoUiState())
    val uiState: StateFlow<TecnicoUiState> = _uiState.asStateFlow()

    private val _events = Channel<TecnicoEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private val geocodingMutex = Mutex()
    private var ultimaGeocodificacionMs = 0L

    init {
        viewModelScope.launch {
            repository.observarSolicitudesDisponibles()
                .catch { error ->
                    _uiState.update {
                        it.copy(cargando = false, error = error.message ?: "No se pudieron cargar las solicitudes.")
                    }
                }
                .collect { solicitudes ->
                    _uiState.update { it.copy(solicitudes = solicitudes, cargando = false, error = null) }
                    cargarReputacionesSolicitudes(solicitudes)
                }
        }
        viewModelScope.launch {
            repository.observarSolicitudesTecnico()
                .catch { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "No se pudieron cargar tus solicitudes.")
                    }
                }
                .collect { solicitudes ->
                    _uiState.update { it.copy(misSolicitudes = solicitudes, cargando = false) }
                    cargarReputacionesSolicitudes(solicitudes)
                }
        }
    }

    private suspend fun cargarReputacionesSolicitudes(solicitudes: List<SolicitudServicio>) {
        val ids = solicitudes.mapNotNull { it.clienteId }
            .distinct()
            .filter { id -> _uiState.value.reputaciones[id] == null }

        if (ids.isEmpty()) return

        val nuevasReputaciones = mutableMapOf<String, ReputacionUsuario>()
        ids.forEach { id ->
            repository.obtenerReputacion(id)
                .onSuccess { reputacion -> nuevasReputaciones[id] = reputacion }
        }

        if (nuevasReputaciones.isNotEmpty()) {
            _uiState.update { it.copy(reputaciones = it.reputaciones + nuevasReputaciones) }
        }
    }

    fun actualizarUbicacion(latitud: Double, longitud: Double) {
        _uiState.update { it.copy(latitud = latitud, longitud = longitud) }
        viewModelScope.launch {
            repository.registrarUbicacionTecnico(latitud, longitud)
                .onFailure { _events.send(TecnicoEvent.ShowMessage(it.message ?: "No se pudo actualizar la ubicación.")) }
        }
    }

    fun registrarTokenFcm() {
        viewModelScope.launch {
            runCatching { FirebaseMessaging.getInstance().token.await() }
                .onSuccess { repository.registrarTokenTecnico(it) }
                .onFailure { _events.send(TecnicoEvent.ShowMessage("No se pudieron activar las notificaciones.")) }
        }
    }

    fun cambiarOrden(orden: OrdenSolicitudes) {
        _uiState.update { it.copy(orden = orden) }
    }

    fun cambiarVista(vista: VistaSolicitudes) {
        _uiState.update { it.copy(vista = vista) }
    }

    fun mostrarMisSolicitudes(mostrar: Boolean) {
        _uiState.update { it.copy(mostrarMisSolicitudes = mostrar, seleccionadaId = null) }
    }

    fun aceptarSolicitud(solicitudId: String) {
        viewModelScope.launch {
            repository.aceptarSolicitud(solicitudId)
                .onSuccess { _events.send(TecnicoEvent.ShowMessage("Solicitud aceptada.")) }
                .onFailure { _events.send(TecnicoEvent.ShowMessage(it.message ?: "No se pudo aceptar la solicitud.")) }
        }
    }

    fun actualizarSolicitud(solicitud: SolicitudServicio, estado: com.fixnow.app.domain.model.EstadoSolicitud, comentario: String) {
        viewModelScope.launch {
            repository.actualizarSolicitudTecnico(solicitud.id, estado, comentario)
                .onSuccess { _events.send(TecnicoEvent.ShowMessage("Solicitud actualizada.")) }
                .onFailure { _events.send(TecnicoEvent.ShowMessage(it.message ?: "No se pudo actualizar la solicitud.")) }
        }
    }

    fun calificarCliente(solicitudId: String, estrellas: Int, comentario: String) {
        viewModelScope.launch {
            repository.calificarCliente(solicitudId, estrellas, comentario)
                .onSuccess { _events.send(TecnicoEvent.ShowMessage("Calificación enviada.")) }
                .onFailure { _events.send(TecnicoEvent.ShowMessage(it.message ?: "No se pudo enviar la calificación.")) }
        }
    }

    fun seleccionarSolicitud(solicitud: SolicitudServicio) {
        _uiState.update { it.copy(seleccionadaId = solicitud.id, vista = VistaSolicitudes.MAPA) }
        if (solicitud.id in _uiState.value.direccionesGeocodificadas) return

        viewModelScope.launch {
            geocodingMutex.withLock {
                val espera = (1100L - (System.currentTimeMillis() - ultimaGeocodificacionMs)).coerceAtLeast(0L)
                delay(espera)
                ultimaGeocodificacionMs = System.currentTimeMillis()
                _uiState.update { it.copy(geocodificandoId = solicitud.id) }
                runCatching {
                    geocodingApi.reverse(latitud = solicitud.latitud, longitud = solicitud.longitud).direccion
                }.onSuccess { direccion ->
                    if (direccion.isNotBlank()) {
                        _uiState.update {
                            it.copy(direccionesGeocodificadas = it.direccionesGeocodificadas + (solicitud.id to direccion))
                        }
                    }
                }.onFailure {
                    _events.send(TecnicoEvent.ShowMessage("No se pudo consultar la dirección en el mapa."))
                }
                _uiState.update { it.copy(geocodificandoId = null) }
            }
        }
    }
}