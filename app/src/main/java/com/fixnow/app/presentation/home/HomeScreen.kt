package com.fixnow.app.presentation.home

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fixnow.app.presentation.theme.FixNowTheme

@Composable
fun HomeScreen(
    displayName: String,
    emailVerified: Boolean,
    onNuevaSolicitud: () -> Unit,
    onProfile: () -> Unit,
    onDesignSystem: () -> Unit,
    onResendVerification: () -> Unit,
    onMisSolicitudes: () -> Unit,
    esTecnico: Boolean,
    esAdmin: Boolean,
    onSolicitudesTecnico: () -> Unit,
    onLogout: () -> Unit,
    fotoBase64: String = ""
) {
    val fotoPerfil = androidx.compose.runtime.remember(fotoBase64) {
        runCatching {
            val bytes = Base64.decode(fotoBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // Barra superior
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FixNow",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.weight(1f))
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .semantics { contentDescription = "Abrir mi perfil" }
                            .clickable(onClick = onProfile),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        if (fotoPerfil != null) {
                            Image(
                                bitmap = fotoPerfil,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = displayName.firstOrNull()?.uppercase() ?: "P",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = if (displayName.isBlank()) {
                        "Hola"
                    } else {
                        "Hola, $displayName"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (esTecnico) "Solicitudes de servicio cercanas" else "¿Qué se te malogró hoy?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!emailVerified) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = "Verifica tu correo: te enviamos un enlace de confirmación.",
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            TextButton(
                                onClick = onResendVerification
                            ) {
                                Text("Reenviar correo")
                            }
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                if (!esTecnico || esAdmin) {
                    Button(
                        onClick = onNuevaSolicitud,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Solicitar servicio")
                    }

                    OutlinedButton(
                        onClick = onMisSolicitudes,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mis solicitudes")
                    }
                }

                if (esTecnico || esAdmin) {
                    Button(
                        onClick = onSolicitudesTecnico,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Solicitudes de técnico")
                    }
                }

                if (esAdmin) {
                    FilledTonalButton(
                        onClick = onDesignSystem,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver sistema de diseño")
                    }
                }

                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar sesión")
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "Home verificado"
)
@Composable
private fun HomeVerifiedPreview() {

    FixNowTheme {

        HomeScreen(
            displayName = "Américo",
            emailVerified = true,
            onNuevaSolicitud = {},
            onProfile = {},
            onDesignSystem = {},
            onResendVerification = {},
            onMisSolicitudes = {},
            esTecnico = false,
            esAdmin = false,
            onSolicitudesTecnico = {},
            onLogout = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "Home correo pendiente"
)
@Composable
private fun HomeNotVerifiedPreview() {

    FixNowTheme {

        HomeScreen(
            displayName = "Américo",
            emailVerified = false,
            onNuevaSolicitud = {},
            onProfile = {},
            onDesignSystem = {},
            onResendVerification = {},
            onMisSolicitudes = {},
            esTecnico = false,
            esAdmin = false,
            onSolicitudesTecnico = {},
            onLogout = {}
        )
    }
}