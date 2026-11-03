package com.example.localbeat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun beatType(size: Int, line: Int, weight: FontWeight, spacing: Float = 0f) =
    TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = weight,
        fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)

val Typography = Typography(
    displaySmall = beatType(36, 42, FontWeight.Bold, -1f),
    headlineLarge = beatType(32, 38, FontWeight.Bold, -0.8f),
    headlineMedium = beatType(28, 34, FontWeight.Bold, -0.6f),
    headlineSmall = beatType(24, 30, FontWeight.Bold, -0.4f),
    titleLarge = beatType(22, 28, FontWeight.SemiBold, -0.3f),
    titleMedium = beatType(16, 22, FontWeight.SemiBold),
    titleSmall = beatType(14, 20, FontWeight.SemiBold),
    bodyLarge = beatType(16, 24, FontWeight.Normal),
    bodyMedium = beatType(14, 20, FontWeight.Normal),
    bodySmall = beatType(12, 18, FontWeight.Normal),
    labelLarge = beatType(14, 20, FontWeight.SemiBold),
    labelMedium = beatType(12, 16, FontWeight.Medium),
    labelSmall = beatType(11, 16, FontWeight.Medium, 0.3f),
)
