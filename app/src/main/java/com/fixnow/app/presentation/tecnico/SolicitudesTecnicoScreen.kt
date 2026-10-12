package com.fixnow.app.presentation.tecnico

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fixnow.app.domain.model.ReputacionUsuario
import com.fixnow.app.domain.model.EstadoSolicitud
import com.fixnow.app.domain.model.SolicitudServicio
import com.fixnow.app.presentation.common.CalificacionDialog
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SolicitudesTecnicoScreen(
    state: TecnicoUiState,
    onSortChange: (OrdenSolicitudes) -> Unit,
    onViewChange: (VistaSolicitudes) -> Unit,
    onSelect: (SolicitudServicio) -> Unit,
    onShowMyRequests: (Boolean) -> Unit,
    onAccept: (String) -> Unit,
    onUpdateRequest: (SolicitudServicio, EstadoSolicitud, String) -> Unit,
    onRateClient: (String, Int, String) -> Unit,
    onBack: () -> Unit
) {
    val cercanas = remember(state.solicitudes, state.latitud, state.longitud, state.orden) {
        val conDistancia = state.solicitudes.map { solicitud ->
            solicitud to state.latitud?.let { lat ->
                state.longitud?.let { lon -> distanciaMetros(lat, lon, solicitud.latitud, solicitud.longitud) }
            }
        }.filter { (_, distancia) -> distancia == null || distancia <= RADIO_CERCANIA_METROS }

        when (state.orden) {
            OrdenSolicitudes.DISTANCIA -> conDistancia.sortedBy { it.second ?: Double.MAX_VALUE }
            OrdenSolicitudes.URGENCIA -> conDistancia.sortedBy { urgenciaOrden(it.first.urgencia) }
            OrdenSolicitudes.RECIENTES -> conDistancia.sortedByDescending { it.first.fechaCreacion }
        }
    }
    val seleccionada = state.solicitudes.firstOrNull { it.id == state.seleccionadaId }
    var menuOrdenAbierto by remember { mutableStateOf(false) }
    var solicitudACalificar by remember { mutableStateOf<SolicitudServicio?>(null) }

    CalificacionDialog(
        visible = solicitudACalificar != null,
        titulo = "Calificar al cliente",
        onDismiss = { solicitudACalificar = null },
        onEnviar = { estrellas, comentario ->
            solicitudACalificar?.let { onRateClient(it.id, estrellas, comentario) }
            solicitudACalificar = null
        }
    )

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onBack) { Text("Volver") }
                Text(
                    if (state.mostrarMisSolicitudes) "Mis solicitudes" else "Solicitudes cercanas",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.size(48.dp))
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(false, true).forEachIndexed { index, mostrarMis ->
                    SegmentedButton(
                        selected = state.mostrarMisSolicitudes == mostrarMis,
                        onClick = { onShowMyRequests(mostrarMis) },
                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index, 2)
                    ) {
                        Text(if (mostrarMis) "Mis solicitudes" else "Cercanas")
                    }
                }
            }

            if (state.mostrarMisSolicitudes) {
                if (state.cargando) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (state.misSolicitudes.isEmpty()) {
                    Text("Aún no has aceptado solicitudes.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.misSolicitudes, key = { it.id }) { solicitud ->
                            SolicitudAsignada(
                                solicitud = solicitud,
                                onUpdate = { estado, comentario -> onUpdateRequest(solicitud, estado, comentario) },
                                onRate = { solicitudACalificar = solicitud }
                            )
                        }
                    }
                }
            } else {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ordenar:", modifier = Modifier.padding(end = 8.dp))
                Box {
                    OutlinedButton(onClick = { menuOrdenAbierto = true }) {
                        Text(state.orden.etiqueta)
                    }
                    DropdownMenu(
                        expanded = menuOrdenAbierto,
                        onDismissRequest = { menuOrdenAbierto = false }
                    ) {
                        OrdenSolicitudes.entries.forEach { orden ->
                            DropdownMenuItem(
                                text = { Text(orden.etiqueta) },
                                onClick = {
                                    menuOrdenAbierto = false
                                    onSortChange(orden)
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("${cercanas.size} solicitudes", style = MaterialTheme.typography.labelLarge)
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                VistaSolicitudes.entries.forEachIndexed { index, vista ->
                    SegmentedButton(
                        selected = state.vista == vista,
                        onClick = { onViewChange(vista) },
                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = VistaSolicitudes.entries.size
                        )
                    ) {
                        Text(if (vista == VistaSolicitudes.LISTA) "Lista" else "Mapa")
                    }
                }
            }

            if (state.latitud == null) {
                Text(
                    "Activa la ubicación para ordenar y recibir avisos de solicitudes dentro de 50 km.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            when {
                state.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Text(state.error, color = MaterialTheme.colorScheme.error)
                state.vista == VistaSolicitudes.MAPA -> {
                    SolicitudesMap(
                        solicitudes = cercanas.map { it.first },
                        solicitudSeleccionadaId = state.seleccionadaId,
                        latitudActual = state.latitud,
                        longitudActual = state.longitud,
                        onSelect = { id -> state.solicitudes.firstOrNull { it.id == id }?.let(onSelect) }
                    )
                    seleccionada?.let { solicitud ->
                        SolicitudDetalle(
                            solicitud = solicitud,
                            distancia = cercanas.firstOrNull { it.first.id == solicitud.id }?.second,
                            direccionGeocodificada = state.direccionesGeocodificadas[solicitud.id],
                            geocodificando = state.geocodificandoId == solicitud.id,
                            reputacionCliente = state.reputaciones[solicitud.clienteId],
                            onAccept = { onAccept(solicitud.id) },
                            onVerEnMapa = { onSelect(solicitud) }
                        )
                    }
                }
                cercanas.isEmpty() -> Text("Todavía no hay solicitudes abiertas cerca de ti.")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cercanas, key = { it.first.id }) { (solicitud, distancia) ->
                        SolicitudDetalle(
                            solicitud = solicitud,
                            distancia = distancia,
                            direccionGeocodificada = state.direccionesGeocodificadas[solicitud.id],
                            geocodificando = state.geocodificandoId == solicitud.id,
                            reputacionCliente = state.reputaciones[solicitud.clienteId],
                            onAccept = { onAccept(solicitud.id) },
                            onVerEnMapa = { onSelect(solicitud) }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
            }
        }
    }
}

@Composable
private fun SolicitudDetalle(
    solicitud: SolicitudServicio,
    distancia: Double?,
    direccionGeocodificada: String?,
    geocodificando: Boolean,
    reputacionCliente: ReputacionUsuario?,
    onAccept: () -> Unit,
    onVerEnMapa: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FotoSolicitud(solicitud.fotoBase64)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(solicitud.categoriaNombre, style = MaterialTheme.typography.titleMedium)
                    Text("${solicitud.urgencia} · S/ ${"%.2f".format(Locale.US, solicitud.precioSugerido)}")
                    if (distancia != null) Text("A ${"%.1f".format(Locale.US, distancia / 1000)} km")
                    Text("${solicitud.fechaCreacion.toFecha()}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(solicitud.descripcion, style = MaterialTheme.typography.bodyMedium)
            Text(
                direccionGeocodificada ?: solicitud.direccion.ifBlank { "Dirección pendiente" },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Cliente: ${solicitud.clienteId} · Estado: ${solicitud.estado}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            reputacionCliente?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("★", color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Reputación del cliente: ${String.format(Locale.US, "%.1f", it.promedio)} / 5 · ${it.totalCalificaciones} valoraciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text("ID: ${solicitud.id}", style = MaterialTheme.typography.labelSmall)
            if (geocodificando) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Consultando dirección…", style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) {
                Text("Aceptar solicitud")
            }
            Button(onClick = onVerEnMapa, modifier = Modifier.fillMaxWidth()) {
                Text("Ver ubicación en mapa")
            }
        }
    }
}

@Composable
private fun SolicitudAsignada(
    solicitud: SolicitudServicio,
    onUpdate: (EstadoSolicitud, String) -> Unit,
    onRate: () -> Unit
) {
    var comentario by rememberSaveable(solicitud.id) { mutableStateOf("") }
    val estado = EstadoSolicitud.entries.firstOrNull { it.name == solicitud.estado }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(solicitud.categoriaNombre, style = MaterialTheme.typography.titleMedium)
            Text("Estado: ${estado?.titulo ?: solicitud.estado}", color = MaterialTheme.colorScheme.primary)
            Text(solicitud.descripcion)
            Text("Cliente: ${solicitud.clienteId}", style = MaterialTheme.typography.bodySmall)
            if (solicitud.comentarioEstado.isNotBlank()) {
                Text("Nota para el cliente: ${solicitud.comentarioEstado}")
            }

            if (estado == EstadoSolicitud.ACEPTADA || estado == EstadoSolicitud.EN_PROCESO) {
                OutlinedTextField(
                    value = comentario,
                    onValueChange = { comentario = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nota de progreso para el cliente") },
                    minLines = 2,
                    maxLines = 4
                )
                TextButton(
                    onClick = {
                        onUpdate(estado, comentario)
                        comentario = ""
                    },
                    enabled = comentario.isNotBlank()
                ) { Text("Guardar comentario") }
                if (estado == EstadoSolicitud.ACEPTADA) {
                    Button(onClick = { onUpdate(EstadoSolicitud.EN_PROCESO, comentario) }) {
                        Text("Iniciar servicio")
                    }
                } else {
                    Button(onClick = { onUpdate(EstadoSolicitud.FINALIZADA, comentario) }) {
                        Text("Dar por finalizado")
                    }
                }
                TextButton(onClick = { onUpdate(EstadoSolicitud.CANCELADA, comentario) }) {
                    Text("Cancelar solicitud", color = MaterialTheme.colorScheme.error)
                }
            }

            if (estado == EstadoSolicitud.FINALIZADA || estado == EstadoSolicitud.CANCELADA) {
                if (!solicitud.calificadoPorTecnico) {
                    OutlinedButton(onClick = onRate, modifier = Modifier.fillMaxWidth()) {
                        Text("Calificar cliente")
                    }
                } else {
                    Text("Cliente calificado", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun FotoSolicitud(fotoBase64: String) {
    val bitmap = remember(fotoBase64) {
        runCatching {
            val bytes = Base64.decode(fotoBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap, contentDescription = "Foto adjunta a la solicitud", modifier = Modifier.size(92.dp))
    } else {
        Box(
            modifier = Modifier.size(92.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Sin foto", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SolicitudesMap(
    solicitudes: List<SolicitudServicio>,
    solicitudSeleccionadaId: String?,
    latitudActual: Double?,
    longitudActual: Double?,
    onSelect: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember(context) { MapView(context).apply { onCreate(Bundle()) } }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    val onSelectActualizado by rememberUpdatedState(onSelect)

    DisposableEffect(mapView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = {
            mapView.getMapAsync { map ->
                map.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) {
                    map.uiSettings.isAttributionEnabled = true
                    map.setOnMarkerClickListener { marker ->
                        marker.snippet?.let(onSelectActualizado)
                        true
                    }
                    mapLibreMap = map
                    styleReady = true
                }
            }
            mapView
        },
        modifier = Modifier.fillMaxWidth().height(320.dp)
    )

    LaunchedEffect(mapLibreMap, styleReady, solicitudes, solicitudSeleccionadaId, latitudActual, longitudActual) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        map.clear()
        solicitudes.forEach { solicitud ->
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(solicitud.latitud, solicitud.longitud))
                    .title(solicitud.categoriaNombre)
                    .snippet(solicitud.id)
            )
        }
        val center = solicitudes.firstOrNull { it.id == solicitudSeleccionadaId }?.let {
            LatLng(it.latitud, it.longitud)
        } ?: if (latitudActual != null && longitudActual != null) {
            LatLng(latitudActual, longitudActual)
        } else {
            solicitudes.firstOrNull()?.let { LatLng(it.latitud, it.longitud) }
        }
        center?.let { map.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(it, 11.0)) }
    }
}

private fun Long.toFecha(): String = if (this <= 0L) "Fecha no disponible" else {
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "PE")).format(Date(this))
}

private fun urgenciaOrden(urgencia: String): Int = when (urgencia.uppercase()) {
    "URGENTE" -> 0
    "PRIORITARIA" -> 1
    else -> 2
}

private fun distanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val radio = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * radio * atan2(sqrt(a), sqrt(1 - a))
}

private const val RADIO_CERCANIA_METROS = 50_000.0
private const val MAP_STYLE_URL = "https://demotiles.maplibre.org/style.json"