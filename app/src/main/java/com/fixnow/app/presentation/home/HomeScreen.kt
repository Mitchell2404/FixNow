package com.fixnow.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
    onLogout: () -> Unit
) {

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

                Text(
                    text = "FixNow",
                    modifier = Modifier.padding(
                        horizontal = 24.dp,
                        vertical = 18.dp
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
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
                    text = "¿Qué se te malogró hoy?",
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

                Button(
                    onClick = onProfile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mi perfil")
                }

                FilledTonalButton(
                    onClick = onDesignSystem,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver sistema de diseño")
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
            onLogout = {}
        )
    }
}