package com.switchtune.app.domain.model

import com.switchtune.app.core.platform.MusicPlatform

/** Minimal metadata about a song, as provided by Odesli (fields may be absent). */
data class Song(
    val title: String?,
    val artist: String?,
    val artworkUrl: String?,
) {
    /** Human-friendly query for the search fallback, e.g. "Title Artist". */
    fun searchQuery(): String =
        listOfNotNull(title?.takeIf { it.isNotBlank() }, artist?.takeIf { it.isNotBlank() })
            .joinToString(" ")
}

/** A resolved link for one platform. [nativeUri] (e.g. "spotify:track:..") opens the native app directly when present. */
data class PlatformLink(
    val platform: MusicPlatform,
    val webUrl: String,
    val nativeUri: String?,
)

/** The full result of resolving a source link through Odesli. */
data class ResolvedSong(
    val song: Song,
    val sourcePlatform: MusicPlatform,
    val links: Map<MusicPlatform, PlatformLink>,
) {
    fun linkFor(platform: MusicPlatform): PlatformLink? = links[platform]
    val availablePlatforms: List<MusicPlatform> get() = links.keys.toList()
}
