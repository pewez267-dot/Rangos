package com.switchtune.app.core.platform

/**
 * The streaming services supported by SwitchTune.
 *
 * - [odesliKey] matches the platform keys returned by the Odesli API in
 *   `linksByPlatform` (e.g. "spotify", "appleMusic").
 * - [packageName] is the Android package of the official app, used to open the
 *   song directly and to check whether the app is installed.
 * - [hostPatterns] are URL host fragments used to detect a link of this platform.
 */
enum class MusicPlatform(
    val odesliKey: String,
    val displayName: String,
    val packageName: String,
    val hostPatterns: List<String>,
) {
    SPOTIFY(
        odesliKey = "spotify",
        displayName = "Spotify",
        packageName = "com.spotify.music",
        hostPatterns = listOf("open.spotify.com", "spotify.link"),
    ),
    YOUTUBE_MUSIC(
        odesliKey = "youtubeMusic",
        displayName = "YouTube Music",
        packageName = "com.google.android.apps.youtube.music",
        hostPatterns = listOf("music.youtube.com"),
    ),
    YOUTUBE(
        odesliKey = "youtube",
        displayName = "YouTube",
        packageName = "com.google.android.youtube",
        hostPatterns = listOf("youtube.com/watch", "youtu.be", "youtube.com/shorts"),
    ),
    APPLE_MUSIC(
        odesliKey = "appleMusic",
        displayName = "Apple Music",
        packageName = "com.apple.android.music",
        hostPatterns = listOf("music.apple.com"),
    ),
    DEEZER(
        odesliKey = "deezer",
        displayName = "Deezer",
        packageName = "deezer.android.app",
        hostPatterns = listOf("deezer.com", "deezer.page.link", "dzr.page.link"),
    ),
    TIDAL(
        odesliKey = "tidal",
        displayName = "Tidal",
        packageName = "com.aspiro.tidal",
        hostPatterns = listOf("tidal.com", "listen.tidal.com"),
    ),
    AMAZON_MUSIC(
        odesliKey = "amazonMusic",
        displayName = "Amazon Music",
        packageName = "com.amazon.mp3",
        hostPatterns = listOf("music.amazon.com", "amazon.com/music"),
    ),
    SOUNDCLOUD(
        odesliKey = "soundcloud",
        displayName = "SoundCloud",
        packageName = "com.soundcloud.android",
        hostPatterns = listOf("soundcloud.com", "snd.sc"),
    ),
    PANDORA(
        odesliKey = "pandora",
        displayName = "Pandora",
        packageName = "com.pandora.android",
        hostPatterns = listOf("pandora.com"),
    ),
    NAPSTER(
        odesliKey = "napster",
        displayName = "Napster",
        packageName = "com.rhapsody.napster",
        hostPatterns = listOf("napster.com"),
    ),
    AUDIOMACK(
        odesliKey = "audiomack",
        displayName = "Audiomack",
        packageName = "com.audiomack",
        hostPatterns = listOf("audiomack.com"),
    ),
    ANGHAMI(
        odesliKey = "anghami",
        displayName = "Anghami",
        packageName = "com.anghami",
        hostPatterns = listOf("anghami.com", "play.anghami.com"),
    );

    companion object {
        fun fromOdesliKey(key: String): MusicPlatform? =
            entries.firstOrNull { it.odesliKey.equals(key, ignoreCase = true) }

        /**
         * Detects which supported platform a URL belongs to, or null if it does
         * not look like a music link we recognise.
         */
        fun detect(url: String): MusicPlatform? {
            val normalized = url.lowercase()
            return entries.firstOrNull { platform ->
                platform.hostPatterns.any { normalized.contains(it) }
            }
        }
    }
}
