package com.switchtune.app.ui.common

import androidx.compose.ui.graphics.Color
import com.switchtune.app.core.platform.MusicPlatform

/**
 * Per-platform visuals using SwitchTune's OWN palette and short text badges
 * (no third-party logos or brand assets). Distinct colors aid recognition.
 */
data class PlatformVisual(val accent: Color, val badge: String)

fun MusicPlatform.visual(): PlatformVisual = when (this) {
    MusicPlatform.SPOTIFY -> PlatformVisual(Color(0xFF2BD576), "S")
    MusicPlatform.APPLE_MUSIC -> PlatformVisual(Color(0xFFFF6B81), "A")
    MusicPlatform.YOUTUBE_MUSIC -> PlatformVisual(Color(0xFFFF5C5C), "YM")
    MusicPlatform.YOUTUBE -> PlatformVisual(Color(0xFFFF8A3D), "YT")
    MusicPlatform.AMAZON_MUSIC -> PlatformVisual(Color(0xFF4DD0E1), "Az")
    MusicPlatform.DEEZER -> PlatformVisual(Color(0xFFA78BFA), "D")
    MusicPlatform.TIDAL -> PlatformVisual(Color(0xFF5C9DFF), "T")
}
