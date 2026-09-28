package com.fixnow.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fixnow.app.presentation.theme.FixNowTheme

@Composable
fun ComposeTest() {

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "FixNow",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Migración a Jetpack Compose",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Button(
                onClick = {}
            ) {
                Text("Continuar")
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "Modo claro"
)
@Composable
private fun ComposeTestLightPreview() {

    FixNowTheme(
        darkTheme = false
    ) {
        ComposeTest()
    }
}

@Preview(
    showBackground = true,
    name = "Modo oscuro"
)
@Composable
private fun ComposeTestDarkPreview() {

    FixNowTheme(
        darkTheme = true
    ) {
        ComposeTest()
    }
}