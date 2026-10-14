package com.sanvalero.boutiquestyle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

private val Espresso = Color(0xFF3E2723)
private val Petal = Color(0xFFF4C9D6)
private val Ivory = Color(0xFFF5F0E6)

private val LightColors = lightColorScheme(
    primary = Espresso,
    onPrimary = Color.White,
    secondary = Petal,
    onSecondary = Espresso,
    tertiary = Petal,
    background = Ivory,
    surface = Ivory,
    onBackground = Espresso,
    onSurface = Espresso
)

private val DarkColors = darkColorScheme(
    primary = Petal,
    onPrimary = Espresso,
    secondary = Petal,
    onSecondary = Espresso,
    tertiary = Petal,
    background = Color(0xFF1F1715),
    surface = Color(0xFF2B211E),
    onBackground = Ivory,
    onSurface = Ivory
)

@Composable
fun BoutiqueTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
