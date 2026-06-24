package com.switchtune.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// SwitchTune always uses a curated dark theme for a premium, focused feel.
private val SwitchTuneColors = darkColorScheme(
    primary = BrandViolet,
    onPrimary = Color.White,
    primaryContainer = BrandViolet,
    onPrimaryContainer = Color.White,
    secondary = BrandMagenta,
    tertiary = BrandCyan,
    background = Background,
    onBackground = OnSurfaceHigh,
    surface = SurfaceDark,
    onSurface = OnSurfaceHigh,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = OnSurfaceMuted,
    outline = OutlineSubtle,
    outlineVariant = OutlineSubtle,
)

@Composable
fun SwitchTuneTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = SwitchTuneColors,
        typography = Typography,
        content = content,
    )
}
