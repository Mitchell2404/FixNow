package com.fixnow.app.presentation.solicitud

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.util.Locale

@Composable
fun SelectorUbicacionDialog(
	visible: Boolean,
	latitudInicial: Double?,
	longitudInicial: Double?,
	onDismiss: () -> Unit,
	onConfirm: (Double, Double) -> Unit
) {
	if (!visible) return

	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current
	val latitud = latitudInicial?.takeIf { it in -90.0..90.0 } ?: LIMA_LATITUD
	val longitud = longitudInicial?.takeIf { it in -180.0..180.0 } ?: LIMA_LONGITUD
	var ubicacionSeleccionada by remember(visible, latitud, longitud) {
		mutableStateOf(LatLng(latitud, longitud))
	}
	var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
	var estiloListo by remember { mutableStateOf(false) }
	val mapView = remember(context) {
		MapView(context).apply { onCreate(Bundle()) }
	}

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

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Elegir ubicación") },
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
				Text(
					"Toca el mapa para mover el marcador al lugar del servicio.",
					style = MaterialTheme.typography.bodySmall
				)
				AndroidView(
					factory = {
						mapView.getMapAsync { map ->
							map.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) {
								map.uiSettings.isAttributionEnabled = true
								map.addOnMapClickListener { punto: LatLng ->
									ubicacionSeleccionada = punto
									true
								}
								mapLibreMap = map
								estiloListo = true
							}
						}
						mapView
					},
					modifier = Modifier.fillMaxWidth().height(320.dp)
				)
				Text(
					String.format(
						Locale.US,
						"Latitud %.5f · Longitud %.5f",
						ubicacionSeleccionada.latitude,
						ubicacionSeleccionada.longitude
					),
					style = MaterialTheme.typography.labelMedium
				)
			}
		},
		confirmButton = {
			TextButton(
				onClick = {
					onConfirm(
						ubicacionSeleccionada.latitude,
						ubicacionSeleccionada.longitude
					)
				}
			) {
				Text("Usar este punto")
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text("Cancelar") }
		}
	)

	LaunchedEffect(mapLibreMap, estiloListo, ubicacionSeleccionada) {
		val map = mapLibreMap ?: return@LaunchedEffect
		if (!estiloListo) return@LaunchedEffect
		map.clear()
		map.addMarker(
			MarkerOptions()
				.position(ubicacionSeleccionada)
				.title("Ubicación del servicio")
		)
		map.animateCamera(
			org.maplibre.android.camera.CameraUpdateFactory
				.newLatLngZoom(ubicacionSeleccionada, 15.0)
		)
	}
}

private const val MAP_STYLE_URL = "https://demotiles.maplibre.org/style.json"
private const val LIMA_LATITUD = -12.0464
private const val LIMA_LONGITUD = -77.0428

