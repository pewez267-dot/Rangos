package com.switchtune.app.core.linkparser

import com.switchtune.app.core.platform.MusicPlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkParserTest {

    @Test
    fun `detects spotify track url`() {
        val parsed = LinkParser.parse("https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT")
        assertEquals(MusicPlatform.SPOTIFY, parsed?.sourcePlatform)
    }

    @Test
    fun `extracts url embedded in shared text`() {
        val parsed = LinkParser.parse("Check this out https://open.spotify.com/track/123?si=abc 🎵")
        assertEquals(MusicPlatform.SPOTIFY, parsed?.sourcePlatform)
        assertEquals("https://open.spotify.com/track/123?si=abc", parsed?.url)
    }

    @Test
    fun `trims trailing punctuation`() {
        val parsed = LinkParser.parse("Look: https://open.spotify.com/track/xyz.")
        assertEquals("https://open.spotify.com/track/xyz", parsed?.url)
    }

    @Test
    fun `maps youtube music host to youtube music`() {
        assertEquals(
            MusicPlatform.YOUTUBE_MUSIC,
            LinkParser.parse("https://music.youtube.com/watch?v=abc")?.sourcePlatform,
        )
    }

    @Test
    fun `maps generic youtube to youtube`() {
        assertEquals(
            MusicPlatform.YOUTUBE,
            LinkParser.parse("https://youtu.be/dQw4w9WgXcQ")?.sourcePlatform,
        )
        assertEquals(
            MusicPlatform.YOUTUBE,
            LinkParser.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ")?.sourcePlatform,
        )
    }

    @Test
    fun `detects youtube music host`() {
        assertEquals(
            MusicPlatform.YOUTUBE_MUSIC,
            LinkParser.parse("https://music.youtube.com/watch?v=abc")?.sourcePlatform,
        )
    }

    @Test
    fun `detects apple deezer tidal amazon`() {
        assertEquals(MusicPlatform.APPLE_MUSIC, LinkParser.parse("https://music.apple.com/us/album/song/1?i=2")?.sourcePlatform)
        assertEquals(MusicPlatform.DEEZER, LinkParser.parse("https://www.deezer.com/track/3135556")?.sourcePlatform)
        assertEquals(MusicPlatform.TIDAL, LinkParser.parse("https://listen.tidal.com/track/12345")?.sourcePlatform)
        assertEquals(MusicPlatform.AMAZON_MUSIC, LinkParser.parse("https://music.amazon.com/albums/B01?trackAsin=B02")?.sourcePlatform)
    }

    @Test
    fun `non music link returns null`() {
        assertNull(LinkParser.parse("https://example.com/article"))
        assertNull(LinkParser.parse("https://github.com/some/repo"))
        assertNull(LinkParser.parse("https://www.google.com/search?q=spotify"))
    }

    @Test
    fun `blank or null returns null`() {
        assertNull(LinkParser.parse(null))
        assertNull(LinkParser.parse(""))
        assertNull(LinkParser.parse("just some text"))
    }
}
