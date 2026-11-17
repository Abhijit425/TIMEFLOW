package com.timeflow.clock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF09090A)
val Panel = Color(0xFF131315)
val Hairline = Color(0x14FFFFFF)
val Ivory = Color(0xFFF2EEE6)
val Amber = Color(0xFFFFB648)

private val TimeflowColors = darkColorScheme(
    primary = Ivory,
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFF2A2824),
    onPrimaryContainer = Ivory,
    secondary = Amber,
    onSecondary = Color(0xFF1E1400),
    secondaryContainer = Color(0xFF3A2C12),
    onSecondaryContainer = Color(0xFFFFDDB0),
    background = Ink,
    onBackground = Ivory,
    surface = Ink,
    onSurface = Ivory,
    surfaceVariant = Panel,
    onSurfaceVariant = Color(0xFFB9B4AC),
    outline = Color(0xFF4A4740),
    outlineVariant = Color(0xFF2A2825),
)

@Composable
fun TimeflowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TimeflowColors,
        typography = Typography(),
        content = content,
    )
}
