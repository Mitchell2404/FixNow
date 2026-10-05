package com.fixnow.app.presentation.solicitud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fixnow.app.domain.model.CategoriaServicio
import com.fixnow.app.domain.model.UrgenciaServicio
import com.fixnow.app.presentation.theme.FixNowTheme
import java.util.Locale
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun SolicitudServicioScreen(
    state: SolicitudServicioUiState,
    onDescripcionChange: (String) -> Unit,
    onCategoriaChange: (CategoriaServicio) -> Unit,
    onUrgenciaChange: (UrgenciaServicio) -> Unit,
    onTomarFoto: () -> Unit,
    onObtenerUbicacion: () -> Unit,
    onDireccionChange: (String) -> Unit,
    onVolver: () -> Unit
) {
    var categoriasAbiertas by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onVolver) {
            Text("Volver")
        }

        Text(
            text = "Solicitar servicio",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Cuéntanos qué problema tiene tu computadora o laptop."
        )

        // Categoría
        Text(
            text = "Tipo de servicio",
            style = MaterialTheme.typography.titleMedium
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { categoriasAbiertas = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.categoria?.titulo
                        ?: "Selecciona una categoría"
                )
            }

            DropdownMenu(
                expanded = categoriasAbiertas,
                onDismissRequest = {
                    categoriasAbiertas = false
                }
            ) {
                CategoriaServicio.entries.forEach { categoria ->
                    DropdownMenuItem(
                        text = {
                            Text(categoria.titulo)
                        },
                        onClick = {
                            onCategoriaChange(categoria)
                            categoriasAbiertas = false
                        }
                    )
                }
            }
        }

        // Descripción
        OutlinedTextField(
            value = state.descripcion,
            onValueChange = onDescripcionChange,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Descripción del problema")
            },
            placeholder = {
                Text("Ejemplo: mi laptop enciende, pero no muestra imagen.")
            },
            minLines = 4,
            maxLines = 6
        )

        //Imagen
        Text(
            text = "Fotografía del equipo",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedButton(
            onClick = onTomarFoto,
            enabled = !state.procesandoFoto
        ) {
            Text(
                text = if (state.fotoBase64.isBlank()) {
                    "Tomar fotografía"
                } else {
                    "Cambiar fotografía"
                }
            )
        }

        if (state.procesandoFoto) {
            CircularProgressIndicator()
        }

        if (state.fotoBase64.isNotBlank()) {
            val imagen = remember(state.fotoBase64) {
                runCatching {
                    val bytes = Base64.decode(
                        state.fotoBase64,
                        Base64.DEFAULT
                    )

                    BitmapFactory.decodeByteArray(
                        bytes,
                        0,
                        bytes.size
                    )?.asImageBitmap()
                }.getOrNull()
            }

            if (imagen != null) {
                Image(
                    bitmap = imagen,
                    contentDescription = "Fotografía del equipo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("No se pudo mostrar la fotografía.")
            }
        }

        state.errorFoto?.let { mensaje ->
            Text(
                text = mensaje,
                color = MaterialTheme.colorScheme.error
            )
        }

        //Ubicacion
        Text(
            text = "Ubicación del servicio",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Obtén la ubicación cuando estés en el lugar donde necesitas la atención."
        )

        OutlinedButton(
            onClick = onObtenerUbicacion,
            enabled = !state.obteniendoUbicacion
        ) {
            Text(
                text = if (state.latitud == null) {
                    "Usar mi ubicación"
                } else {
                    "Actualizar ubicación"
                }
            )
        }

        if (state.obteniendoUbicacion) {
            CircularProgressIndicator()
            Text("Obteniendo ubicación…")
        }

        if (state.latitud != null && state.longitud != null) {
            Text("Ubicación registrada")

            Text(
                text = String.format(
                    java.util.Locale.US,
                    "Latitud: %.5f\nLongitud: %.5f",
                    state.latitud,
                    state.longitud
                )
            )

            if (state.ubicacionAproximada) {
                Text(
                    text = "El permiso concedido permite una ubicación aproximada. " +
                            "Puedes habilitar la ubicación precisa en los ajustes de la aplicación."
                )
            }
        }

        state.errorUbicacion?.let { mensaje ->
            Text(
                text = mensaje,
                color = MaterialTheme.colorScheme.error
            )
        }

        OutlinedTextField(
            value = state.direccion,
            onValueChange = onDireccionChange,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Dirección y referencia")
            },
            placeholder = {
                Text("Ejemplo: Av. Los Olivos 123, segundo piso.")
            },
            minLines = 2,
            maxLines = 3
        )

        // Urgencia
        Text(
            text = "¿Qué tan pronto necesitas atención?",
            style = MaterialTheme.typography.titleMedium
        )

        Column(
            modifier = Modifier.selectableGroup()
        ) {
            UrgenciaServicio.entries.forEach { urgencia ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = state.urgencia == urgencia,
                            role = Role.RadioButton,
                            onClick = {
                                onUrgenciaChange(urgencia)
                            }
                        )
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = state.urgencia == urgencia,
                        onClick = null
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = urgencia.titulo,
                            style = MaterialTheme.typography.titleSmall
                        )

                        Text(
                            text = urgencia.descripcion,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        if (state.urgencia == UrgenciaServicio.URGENTE) {
            Text(
                text = "La atención depende de la disponibilidad "
                        + "de técnicos en tu zona.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Precio
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Precio referencial",
                    style = MaterialTheme.typography.titleMedium
                )

                val precio = state.precioSugerido

                if (precio == null) {
                    Text("Selecciona una categoría para ver la estimación.")
                } else {
                    Text(
                        text = String.format(
                            Locale("es", "PE"),
                            "S/ %.2f",
                            precio
                        ),
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = "Incluye el recargo por urgencia, "
                                + "cuando corresponde.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = "Estimación de atención y mano de obra. "
                            + "No incluye repuestos ni licencias. "
                            + "El técnico confirmará el costo antes del trabajo.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SolicitudServicioScreenPreview() {
    FixNowTheme {
        SolicitudServicioScreen(
            state = SolicitudServicioUiState(
                categoria = CategoriaServicio.MANTENIMIENTO,
                precioSugerido = 60.0
            ),
            onDescripcionChange = {},
            onCategoriaChange = {},
            onUrgenciaChange = {},
            onTomarFoto = {},
            onObtenerUbicacion = {},
            onDireccionChange = {},
            onVolver = {}
        )
    }
}