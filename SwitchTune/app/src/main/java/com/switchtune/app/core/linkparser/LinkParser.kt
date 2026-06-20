package com.switchtune.app.core.linkparser

import com.switchtune.app.core.platform.MusicPlatform

/**
 * Extracts and validates a music URL from arbitrary text (shared text or
 * clipboard contents). Real-world shares often include extra text, e.g.
 * "Check this out https://open.spotify.com/track/123 🎵", so we extract the
 * first URL and then check whether it belongs to a supported platform.
 */
object LinkParser {

    private val urlRegex = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)

    data class ParsedLink(val url: String, val sourcePlatform: MusicPlatform)

    /**
     * Returns a [ParsedLink] if [text] contains a URL from a supported music
     * platform, otherwise null. Null means "not a music link" and the UI should
     * show the normal empty state, never an error.
     */
    fun parse(text: String?): ParsedLink? {
        if (text.isNullOrBlank()) return null
        for (match in urlRegex.findAll(text)) {
            val candidate = match.value.trimEnd('.', ',', ')', ']', '"', '\'', '>')
            val platform = MusicPlatform.detect(candidate) ?: continue
            return ParsedLink(candidate, platform)
        }
        return null
    }

    fun isMusicLink(text: String?): Boolean = parse(text) != null
}
