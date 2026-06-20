package com.switchtune.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Waves
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.switchtune.app.core.platform.MusicPlatform

/**
 * Per-platform visuals using SwitchTune's OWN design language: a two-tone
 * gradient and a professional vector glyph from the open-source Material icon
 * family. No third-party logos or brand assets are used.
 */
data class PlatformVisual(
    val gradient: List<Color>,
    val icon: ImageVector,
)

fun MusicPlatform.visual(): PlatformVisual = when (this) {
    MusicPlatform.SPOTIFY -> PlatformVisual(
        gradient = listOf(Color(0xFF34E27A), Color(0xFF0E9E50)),
        icon = Icons.Filled.GraphicEq,
    )
    MusicPlatform.APPLE_MUSIC -> PlatformVisual(
        gradient = listOf(Color(0xFFFB5C74), Color(0xFFB13C8E)),
        icon = Icons.Filled.Album,
    )
    MusicPlatform.YOUTUBE_MUSIC -> PlatformVisual(
        gradient = listOf(Color(0xFFFF6A6A), Color(0xFFC81E1E)),
        icon = Icons.Filled.Equalizer,
    )
    MusicPlatform.YOUTUBE -> PlatformVisual(
        gradient = listOf(Color(0xFFFF7A45), Color(0xFFE2323F)),
        icon = Icons.Filled.PlayArrow,
    )
    MusicPlatform.AMAZON_MUSIC -> PlatformVisual(
        gradient = listOf(Color(0xFF45D6E5), Color(0xFF2A7DE1)),
        icon = Icons.Filled.LibraryMusic,
    )
    MusicPlatform.DEEZER -> PlatformVisual(
        gradient = listOf(Color(0xFFB98CFF), Color(0xFF7A3CF0)),
        icon = Icons.Filled.QueueMusic,
    )
    MusicPlatform.TIDAL -> PlatformVisual(
        gradient = listOf(Color(0xFF7FC2FF), Color(0xFF1E5BC6)),
        icon = Icons.Filled.Waves,
    )
}
