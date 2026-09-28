package com.fixnow.app.presentation.profile

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fixnow.app.domain.model.User
import com.fixnow.app.presentation.theme.FixNowTheme

@Composable
fun ProfileScreen(
    user: User?,
    email: String,
    isLoading: Boolean,
    biometricEnabled: Boolean,
    biometricSupported: Boolean,
    onTakePhoto: () -> Unit,
    onSaveProfile: (String, Boolean) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onBiometricChanged: (Boolean) -> Unit
) {

    var name by rememberSaveable {
        mutableStateOf("")
    }

    var notificationsEnabled by rememberSaveable {
        mutableStateOf(true)
    }

    /*
     * Solo actualizamos el formulario cuando cambia de usuario.
     * Así una recomposición no borra lo que el usuario está escribiendo.
     */
    LaunchedEffect(user?.uid) {

        user?.let {
            name = it.nombre
            notificationsEnabled = it.notificacionesActivas
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        ProfilePhoto(
            fotoBase64 = user?.fotoBase64.orEmpty()
        )

        TextButton(
            onClick = onTakePhoto,
            enabled = !isLoading
        ) {
            Text("Tomar foto")
        }

        Text(
            text = email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Nombre")
            },
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Recibir notificaciones",
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = notificationsEnabled,
                onCheckedChange = { checked ->

                    notificationsEnabled = checked
                    onNotificationsChanged(checked)
                },
                enabled = !isLoading
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = if (biometricSupported) {
                    "Iniciar sesión con huella dactilar"
                } else {
                    "Huella dactilar no disponible"
                },
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = biometricEnabled && biometricSupported,
                onCheckedChange = onBiometricChanged,
                enabled = biometricSupported && !isLoading
            )
        }

        Button(
            onClick = {
                onSaveProfile(
                    name.trim(),
                    notificationsEnabled
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            enabled = !isLoading
        ) {
            Text("Guardar cambios")
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun ProfilePhoto(
    fotoBase64: String
) {

    val imageBitmap = remember(fotoBase64) {

        if (fotoBase64.isBlank()) {

            null

        } else {

            runCatching {

                val bytes = Base64.decode(
                    fotoBase64,
                    Base64.DEFAULT
                )

                BitmapFactory.decodeByteArray(
                    bytes,
                    0,
                    bytes.size
                )?.asImageBitmap()

            }.getOrNull()
        }
    }

    Box(
        modifier = Modifier
            .padding(top = 24.dp)
            .size(120.dp)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center
    ) {

        if (imageBitmap != null) {

            Image(
                bitmap = imageBitmap,
                contentDescription = "Foto de perfil",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

        } else {

            Text(
                text = "Sin foto",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(
    showBackground = true,
    name = "Perfil"
)
@Composable
private fun ProfileScreenPreview() {

    FixNowTheme {

        ProfileScreen(
            user = User(
                uid = "1",
                nombre = "Nombre...",
                email = "prueba@email.com",
                notificacionesActivas = true
            ),
            email = "prueba@email.com",
            isLoading = false,
            biometricEnabled = true,
            biometricSupported = true,
            onTakePhoto = {},
            onSaveProfile = { _, _ -> },
            onNotificationsChanged = {},
            onBiometricChanged = {}
        )
    }
}