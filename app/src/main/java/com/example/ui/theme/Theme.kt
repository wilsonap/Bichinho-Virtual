package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PetAmber,
    onPrimary = Color.Black,
    primaryContainer = PetOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = PetTeal,
    onSecondary = Color.White,
    background = PetDarkBg,
    surface = PetDarkSurface,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PetOrange,
    onPrimary = Color.White,
    primaryContainer = PetAmberLight,
    onPrimaryContainer = Color(0xFF78350F),
    secondary = PetTeal,
    onSecondary = Color.White,
    secondaryContainer = PetGreenLight,
    onSecondaryContainer = Color(0xFF065F46),
    background = PetBgLight,
    surface = PetSurfaceLight,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to preserve custom game palette
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

