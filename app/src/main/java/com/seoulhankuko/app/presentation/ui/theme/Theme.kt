package com.seoulhankuko.app.presentation.ui.theme

import android.app.Activity
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
    primary = Green80,
    secondary = GreenGrey80,
    tertiary = GreenAccent80,
    background = Color(0xFF1B5E20), // Xanh lá đậm cho background
    surface = Color(0xFF2E7D32), // Xanh lá đậm cho surface
    surfaceVariant = Color(0xFF388E3C), // Xanh lá cho surface variant
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    secondary = GreenGrey40,
    tertiary = GreenAccent40,
    background = WhiteBackground, // White background
    surface = CardBackground, // Light gray for cards and surfaces
    surfaceVariant = LightGrayBackground, // Very light gray for surface variants
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1B5E20), // Dark green for text on white background
    onSurface = Color(0xFF2E7D32), // Medium green for text on surfaces
    onSurfaceVariant = Color(0xFF388E3C), // Green for variant text
    error = Color(0xFFD32F2F), // Red for errors
    onError = Color.White, // White text on error background
    outline = Color(0xFFE0E0E0) // Light gray for outlines
)

@Composable
fun SeoulhankukobookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}