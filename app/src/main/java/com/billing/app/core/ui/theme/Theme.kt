package com.billing.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    background = BackgroundColor,
    surface = SurfaceColor,
    error = ErrorColor,
    onPrimary = SurfaceColor,
    onSecondary = SurfaceColor,
    onBackground = TextPrimaryColor,
    onSurface = TextPrimaryColor,
    onError = SurfaceColor
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBrand,
    secondary = SecondaryColor,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    error = ErrorColor,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    onError = Color.White
)

@Composable
fun BillingAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
