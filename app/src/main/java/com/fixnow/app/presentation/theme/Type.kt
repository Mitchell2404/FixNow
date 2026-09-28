package com.fixnow.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

private val DefaultTypography = Typography()

val FixNowTypography = Typography(

    headlineMedium = DefaultTypography.headlineMedium.copy(
        fontWeight = FontWeight.Medium
    ),

    titleLarge = DefaultTypography.titleLarge.copy(
        fontWeight = FontWeight.Medium
    ),

    bodyLarge = DefaultTypography.bodyLarge.copy(
        fontWeight = FontWeight.Normal
    ),

    labelLarge = DefaultTypography.labelLarge.copy(
        fontWeight = FontWeight.Medium
    )
)