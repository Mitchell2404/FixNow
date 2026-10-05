package com.fixnow.app.presentation.solicitud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fixnow.app.presentation.theme.FixNowTheme

@Composable
fun SolicitudServicioScreen(
    onVolver: () -> Unit
) {
    var descripcion by rememberSaveable {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(
            onClick = onVolver
        ) {
            Text("Volver")
        }

        Text(
            text = "Solicitar servicio",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Cuéntanos qué problema tiene tu computadora o laptop.",
            style = MaterialTheme.typography.bodyLarge
        )

        OutlinedTextField(
            value = descripcion,
            onValueChange = { nuevoTexto ->
                descripcion = nuevoTexto
            },
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
    }
}

@Preview(showBackground = true)
@Composable
private fun SolicitudServicioScreenPreview() {
    FixNowTheme {
        SolicitudServicioScreen(
            onVolver = {}
        )
    }
}