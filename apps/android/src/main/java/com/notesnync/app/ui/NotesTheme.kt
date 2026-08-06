package com.notesnync.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.notesnync.app.branding.BrandPalette
import com.notesnync.app.branding.palette
import com.notesnync.app.domain.AppSettings
import com.notesnync.app.domain.ThemeMode

/**
 * Neutral canvas stays legible on both themes; the selected [BrandPalette] tints the accent,
 * secondary/tertiary, and a faint wash into the surfaces so the whole app feels "adaptive".
 */
private fun lightScheme(p: BrandPalette) = lightColorScheme(
    primary = p.accent,
    onPrimary = p.onAccent,
    secondary = p.c2,
    tertiary = p.c4,
    background = lerp(Color(0xFFF7F6FB), p.c1, 0.04f),
    onBackground = Color(0xFF1B1830),
    surface = Color.White,
    onSurface = Color(0xFF211E33),
    surfaceVariant = lerp(Color(0xFFEFECF7), p.accent, 0.10f),
    onSurfaceVariant = Color(0xFF6B677A),
    outline = Color(0xFFE2DEEC),
    primaryContainer = lerp(Color.White, p.accent, 0.14f),
    onPrimaryContainer = lerp(Color(0xFF1B1830), p.accent, 0.35f),
)

private fun darkScheme(p: BrandPalette) = darkColorScheme(
    primary = lerp(p.accent, Color.White, 0.14f),
    onPrimary = Color(0xFF120E24),
    secondary = p.c1,
    tertiary = p.c4,
    background = lerp(Color(0xFF0C0A16), p.c3, 0.05f),
    onBackground = Color(0xFFECE9F5),
    surface = lerp(Color(0xFF16131F), p.c3, 0.05f),
    onSurface = Color(0xFFECE9F5),
    surfaceVariant = lerp(Color(0xFF23202E), p.accent, 0.12f),
    onSurfaceVariant = Color(0xFFA9A4B8),
    outline = Color(0xFF373345),
    primaryContainer = lerp(Color(0xFF23202E), p.accent, 0.28f),
    onPrimaryContainer = Color(0xFFF2EEFB),
)

@Composable
fun NotesNyncTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val palette = settings.themeProfile.palette()
    val dark = when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val density = LocalDensity.current
    val scale = settings.uiScale.coerceIn(0.78f, 1.12f)
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density * scale,
            fontScale = density.fontScale * scale,
        ),
    ) {
        MaterialTheme(colorScheme = if (dark) darkScheme(palette) else lightScheme(palette), content = content)
    }
}
