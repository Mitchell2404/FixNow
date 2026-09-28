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
import androidx.compose.material3.OutlinedButton
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
fun LoginScreen(
    isLoading: Boolean,
    showBiometricLogin: Boolean,
    onLogin: (String, String) -> Unit,
    onForgotPassword: (String) -> Unit,
    onRegister: () -> Unit,
    onBiometricLogin: () -> Unit
) {

    var email by rememberSaveable {
        mutableStateOf("")
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    var passwordVisible by rememberSaveable {
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
            text = "FixNow",
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

        TextButton(
            onClick = {
                onForgotPassword(email.trim())
            },
            modifier = Modifier.align(Alignment.End),
            enabled = !isLoading
        ) {
            Text("¿Olvidaste tu contraseña?")
        }

        Button(
            onClick = {
                onLogin(
                    email.trim(),
                    password
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Iniciar sesión")
        }

        if (showBiometricLogin) {

            OutlinedButton(
                onClick = onBiometricLogin,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("Ingresar con huella dactilar")
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(8.dp)
            )
        }

        TextButton(
            onClick = onRegister,
            enabled = !isLoading
        ) {
            Text("¿No tienes cuenta? Regístrate")
        }
    }
}


@Preview(
    showBackground = true,
    name = "Login claro"
)
@Composable
private fun LoginScreenLightPreview() {

    FixNowTheme(
        darkTheme = false
    ) {
        LoginScreen(
            isLoading = false,
            showBiometricLogin = true,
            onLogin = { _, _ -> },
            onForgotPassword = {},
            onRegister = {},
            onBiometricLogin = {}
        )
    }
}


@Preview(
    showBackground = true,
    name = "Login oscuro"
)
@Composable
private fun LoginScreenDarkPreview() {

    FixNowTheme(
        darkTheme = true
    ) {
        LoginScreen(
            isLoading = false,
            showBiometricLogin = true,
            onLogin = { _, _ -> },
            onForgotPassword = {},
            onRegister = {},
            onBiometricLogin = {}
        )
    }
}