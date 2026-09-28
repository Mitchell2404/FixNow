package com.fixnow.app.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fixnow.app.presentation.theme.FixNowTheme

@Composable
fun RegisterScreen(
    isLoading: Boolean,
    onRegister: (String, String, String) -> Unit,
    onGoToLogin: () -> Unit
) {

    var email by rememberSaveable {
        mutableStateOf("")
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    var confirmPassword by rememberSaveable {
        mutableStateOf("")
    }

    var passwordVisible by rememberSaveable {
        mutableStateOf(false)
    }

    var confirmPasswordVisible by rememberSaveable {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Crear cuenta",
            modifier = Modifier.padding(
                top = 48.dp,
                bottom = 20.dp
            ),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Correo electrónico")
            },
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            )
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Contraseña")
            },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation =
                if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            trailingIcon = {
                TextButton(
                    onClick = {
                        passwordVisible = !passwordVisible
                    }
                ) {
                    Text(
                        if (passwordVisible) {
                            "Ocultar"
                        } else {
                            "Mostrar"
                        }
                    )
                }
            }
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Confirmar contraseña")
            },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation =
                if (confirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            trailingIcon = {
                TextButton(
                    onClick = {
                        confirmPasswordVisible = !confirmPasswordVisible
                    }
                ) {
                    Text(
                        if (confirmPasswordVisible) {
                            "Ocultar"
                        } else {
                            "Mostrar"
                        }
                    )
                }
            }
        )

        Button(
            onClick = {
                onRegister(
                    email.trim(),
                    password,
                    confirmPassword
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            enabled = !isLoading
        ) {
            Text("Registrarme")
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(8.dp)
            )
        }

        TextButton(
            onClick = onGoToLogin,
            enabled = !isLoading
        ) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}

@Preview(
    showBackground = true,
    name = "Registro claro"
)
@Composable
private fun RegisterScreenLightPreview() {

    FixNowTheme(
        darkTheme = false
    ) {
        RegisterScreen(
            isLoading = false,
            onRegister = { _, _, _ -> },
            onGoToLogin = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "Registro oscuro"
)
@Composable
private fun RegisterScreenDarkPreview() {

    FixNowTheme(
        darkTheme = true
    ) {
        RegisterScreen(
            isLoading = false,
            onRegister = { _, _, _ -> },
            onGoToLogin = {}
        )
    }
}