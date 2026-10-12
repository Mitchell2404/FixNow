package com.fixnow.app.presentation.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CalificacionDialog(
    visible: Boolean,
    titulo: String,
    onDismiss: () -> Unit,
    onEnviar: (Int, String) -> Unit
) {
    if (!visible) return

    var estrellas by rememberSaveable { mutableStateOf(0) }
    var comentario by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(visible) {
        if (visible) {
            estrellas = 0
            comentario = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = titulo)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(5) { index ->
                        val selected = index < estrellas
                        Text(
                            text = if (selected) "★" else "☆",
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .clickable { estrellas = index + 1 }
                                .padding(2.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = comentario,
                    onValueChange = { comentario = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Comentario") },
                    minLines = 3,
                    maxLines = 5
                )

            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onEnviar(estrellas, comentario.trim())
                    onDismiss()
                },
                enabled = estrellas in 1..5
            ) {
                Text("Enviar calificación")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
