package com.switchtune.app.data.odesli

import com.switchtune.app.data.odesli.dto.OdesliResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Odesli (song.link) public API.
 *
 * Without a registered API key the public limit is ~10 requests/minute; a 429
 * response must be handled gracefully (see [OdesliRepository]). For production,
 * request a key from developers@song.link and pass it via [apiKey].
 */
interface OdesliApi {

    @GET("v1-alpha.1/links")
    suspend fun getLinks(
        @Query("url") url: String,
        @Query("userCountry") userCountry: String = "US",
        @Query("songIfSingle") songIfSingle: Boolean = true,
        @Query("key") apiKey: String? = null,
    ): OdesliResponseDto

    companion object {
        const val BASE_URL = "https://api.song.link/"
    }
}
