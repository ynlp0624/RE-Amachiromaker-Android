package cc.nekoha.amachiromaker.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Flat white/light-blue palette, matching the fork's restyled web look. */
private val LightColors = lightColorScheme(
    primary = Color(0xFF3E93E0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6EAFB),
    onPrimaryContainer = Color(0xFF10344F),
    secondary = Color(0xFF6FB3E8),
    onSecondary = Color(0xFF06263C),
    background = Color(0xFFF4FAFF),
    onBackground = Color(0xFF10293D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF10293D),
    surfaceVariant = Color(0xFFE7F2FD),
    onSurfaceVariant = Color(0xFF3C5A74),
    outline = Color(0xFFBFDCF3),
    outlineVariant = Color(0xFFD8E9F8),
    error = Color(0xFFD9576A),
    onError = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BC7F6),
    onPrimary = Color(0xFF062033),
    primaryContainer = Color(0xFF1C3D57),
    onPrimaryContainer = Color(0xFFD3E9FB),
    secondary = Color(0xFF7FB6DF),
    onSecondary = Color(0xFF041E30),
    background = Color(0xFF0D1822),
    onBackground = Color(0xFFDCEBF8),
    surface = Color(0xFF152431),
    onSurface = Color(0xFFDCEBF8),
    surfaceVariant = Color(0xFF1E3244),
    onSurfaceVariant = Color(0xFFB3CCE1),
    outline = Color(0xFF33536E),
    outlineVariant = Color(0xFF25405A),
    error = Color(0xFFF08A9B),
    onError = Color(0xFF3A0C15)
)

@Composable
fun AmachiromakerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
