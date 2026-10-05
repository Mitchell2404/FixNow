package com.fixnow.app.presentation.solicitud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fixnow.app.domain.model.EstadoSolicitud
import com.fixnow.app.domain.model.SolicitudServicio
import com.fixnow.app.domain.model.UrgenciaServicio
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun MisSolicitudesScreen(
    state: MisSolicitudesUiState,
    onActualizar: () -> Unit,
    onCancelarSolicitud: (String) -> Unit,
    onVolver: () -> Unit
) {
    var solicitudPorCancelar by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var mostrarAvisoTecnicos by rememberSaveable {
        mutableStateOf(false)
    }

    val idPorCancelar = solicitudPorCancelar

    if (idPorCancelar != null) {
        AlertDialog(
            onDismissRequest = {
                solicitudPorCancelar = null
            },
            title = {
                Text("Cancelar solicitud")
            },
            text = {
                Text(
                    "¿Deseas cancelar esta solicitud? " +
                            "Si luego necesitas el servicio, deberás crear una nueva."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        solicitudPorCancelar = null
                        onCancelarSolicitud(idPorCancelar)
                    }
                ) {
                    Text("Sí, cancelar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        solicitudPorCancelar = null
                    }
                ) {
                    Text("Conservar solicitud")
                }
            }
        )
    }

    if (mostrarAvisoTecnicos) {
        AlertDialog(
            onDismissRequest = {
                mostrarAvisoTecnicos = false
            },
            title = {
                Text("Técnicos interesados")
            },
            text = {
                Text(
                    "Esta función estará disponible cuando se conecten " +
                            "las respuestas de los técnicos."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarAvisoTecnicos = false
                    }
                ) {
                    Text("Entendido")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onVolver) {
                Text("Volver")
            }

            OutlinedButton(
                onClick = onActualizar,
                enabled = !state.cargando && state.cancelandoId == null
            ) {
                Text("Actualizar")
            }
        }

        Text(
            text = "Mis solicitudes",
            style = MaterialTheme.typography.headlineMedium
        )

        if (state.cargando) {
            CircularProgressIndicator()
        }

        state.error?.let { mensaje ->
            Text(
                text = mensaje,
                color = MaterialTheme.colorScheme.error
            )
        }

        state.mensaje?.let { mensaje ->
            Text(
                text = mensaje,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (
            !state.cargando &&
            state.error == null &&
            state.solicitudes.isEmpty()
        ) {
            Text("Todavía no has registrado solicitudes.")
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = state.solicitudes,
                key = { it.id }
            ) {  solicitud -> SolicitudCard(
                solicitud = solicitud,
                cancelando = state.cancelandoId == solicitud.id,
                accionesHabilitadas = !state.cargando &&
                        state.cancelandoId == null,
                onCancelar = {
                    solicitudPorCancelar = solicitud.id
                },
                onVerTecnicos = {
                    mostrarAvisoTecnicos = true
                }
            )
            }
        }
    }
}

@Composable
private fun SolicitudCard(solicitud: SolicitudServicio,
                          cancelando: Boolean,
                          accionesHabilitadas: Boolean,
                          onCancelar: () -> Unit,
                          onVerTecnicos: () -> Unit) {
    val estado = EstadoSolicitud.entries.firstOrNull {
        it.name == solicitud.estado
    }?.titulo ?: solicitud.estado.ifBlank { "Sin estado" }

    val urgencia = UrgenciaServicio.entries.firstOrNull {
        it.name == solicitud.urgencia
    }?.titulo ?: solicitud.urgencia

    val fecha = if (solicitud.fechaCreacion > 0L) {
        SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale("es", "PE")
        ).format(Date(solicitud.fechaCreacion))
    } else {
        "Fecha no disponible"
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = solicitud.categoriaNombre,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = fecha,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Estado: $estado",
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = solicitud.descripcion,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text("Urgencia: $urgencia")

            Text(
                text = String.format(
                    Locale("es", "PE"),
                    "Precio referencial: S/ %.2f",
                    solicitud.precioSugerido
                )
            )
            if (
                solicitud.estado == EstadoSolicitud.PUBLICADA.name &&
                solicitud.tecnicoId == null
            ) {
                OutlinedButton(
                    onClick = onVerTecnicos,
                    enabled = accionesHabilitadas,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver técnicos interesados")
                }

                TextButton(
                    onClick = onCancelar,
                    enabled = accionesHabilitadas,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (cancelando) {
                            "Cancelando…"
                        } else {
                            "Cancelar solicitud"
                        },
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}