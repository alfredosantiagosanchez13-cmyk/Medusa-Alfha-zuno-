package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZunoDarkColorScheme = darkColorScheme(
    primary = ZunoGold,
    onPrimary = Color.Black,
    primaryContainer = ZunoGoldDark,
    onPrimaryContainer = ZunoGoldBright,
    secondary = MetallicGrayLight,
    onSecondary = Color.Black,
    secondaryContainer = MetallicGrayDark,
    onSecondaryContainer = Color.White,
    tertiary = StudioCyan,
    onTertiary = Color.Black,
    background = ZunoBlack,
    onBackground = Color(0xFFF0F1F5),
    surface = ZunoSurfaceDark,
    onSurface = Color(0xFFE8EAF0),
    surfaceVariant = ZunoSurfaceCard,
    onSurfaceVariant = MetallicGrayLight,
    outline = ZunoBorderMetallic,
    error = StudioRedClipping
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ZunoDarkColorScheme,
        typography = Typography,
        content = content
    )
}
