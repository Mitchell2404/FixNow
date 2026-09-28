package com.fixnow.app.presentation.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
fun DesignSystemScreen() {

    var exampleText by rememberSaveable {
        mutableStateOf("")
    }

    var switchEnabled by rememberSaveable {
        mutableStateOf(true)
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Text(
            text = "Sistema de diseño",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Cambia el teléfono a modo oscuro para ver la otra paleta.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SectionTitle("Paleta")

        ColorSample(
            text = "Primary",
            backgroundColor = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onPrimary
        )

        ColorSample(
            text = "Primary container",
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            textColor = MaterialTheme.colorScheme.onPrimaryContainer
        )

        ColorSample(
            text = "Secondary",
            backgroundColor = MaterialTheme.colorScheme.secondary,
            textColor = MaterialTheme.colorScheme.onSecondary
        )

        ColorSample(
            text = "Secondary container",
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
            textColor = MaterialTheme.colorScheme.onSecondaryContainer
        )

        ColorSample(
            text = "Tertiary",
            backgroundColor = MaterialTheme.colorScheme.tertiary,
            textColor = MaterialTheme.colorScheme.onTertiary
        )

        ColorSample(
            text = "Tertiary container",
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
            textColor = MaterialTheme.colorScheme.onTertiaryContainer
        )

        ColorSample(
            text = "Error",
            backgroundColor = MaterialTheme.colorScheme.error,
            textColor = MaterialTheme.colorScheme.onError
        )

        ColorSample(
            text = "Surface variant",
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SectionTitle("Tipografía")

        Text(
            text = "Headline medium",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Title large",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Body large: texto normal de párrafos y descripciones.",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "Label large",
            style = MaterialTheme.typography.labelLarge
        )

        SectionTitle("Botones")

        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Relleno (principal)")
        }

        FilledTonalButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tonal")
        }

        OutlinedButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Con borde")
        }

        TextButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Solo texto")
        }

        SectionTitle("Campo de texto")

        OutlinedTextField(
            value = exampleText,
            onValueChange = {
                exampleText = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Describe el problema")
            }
        )

        SectionTitle("Card, chips e interruptor")

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Reparación de refrigeradora",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Así se verá una solicitud dentro de una card.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    AssistChip(
                        onClick = {},
                        label = {
                            Text("Electrodoméstico")
                        }
                    )

                    AssistChip(
                        onClick = {},
                        label = {
                            Text("Urgente")
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = "Interruptor",
                modifier = Modifier.padding(top = 12.dp)
            )

            Switch(
                checked = switchEnabled,
                onCheckedChange = {
                    switchEnabled = it
                }
            )
        }
    }
}

@Composable
private fun SectionTitle(
    text: String
) {

    Text(
        text = text,
        modifier = Modifier.padding(
            top = 20.dp,
            bottom = 4.dp
        ),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ColorSample(
    text: String,
    backgroundColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color
) {

    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(14.dp),
        color = textColor
    )
}

@Preview(
    showBackground = true,
    name = "Design System Claro"
)
@Composable
private fun DesignSystemLightPreview() {

    FixNowTheme(
        darkTheme = false
    ) {
        DesignSystemScreen()
    }
}

@Preview(
    showBackground = true,
    name = "Design System Oscuro"
)
@Composable
private fun DesignSystemDarkPreview() {

    FixNowTheme(
        darkTheme = true
    ) {
        DesignSystemScreen()
    }
}