package com.switchtune.app.data.odesli

import com.switchtune.app.BuildConfig
import com.switchtune.app.core.platform.MusicPlatform
import com.switchtune.app.domain.model.PlatformLink
import com.switchtune.app.domain.model.ResolveResult
import com.switchtune.app.domain.model.ResolvedSong
import com.switchtune.app.domain.model.Song
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface OdesliRepository {
    suspend fun resolve(url: String, sourcePlatform: MusicPlatform): ResolveResult
}

@Singleton
class OdesliRepositoryImpl @Inject constructor(
    private val api: OdesliApi,
) : OdesliRepository {

    override suspend fun resolve(url: String, sourcePlatform: MusicPlatform): ResolveResult {
        return try {
            val apiKey = BuildConfig.ODESLI_API_KEY.ifBlank { null }
            val dto = api.getLinks(url = url, apiKey = apiKey)

            val links = dto.linksByPlatform.mapNotNull { (key, link) ->
                val platform = MusicPlatform.fromOdesliKey(key) ?: return@mapNotNull null
                val webUrl = link.url ?: return@mapNotNull null
                platform to PlatformLink(
                    platform = platform,
                    webUrl = webUrl,
                    nativeUri = link.nativeAppUriMobile,
                )
            }.toMap()

            if (links.isEmpty()) return ResolveResult.NotFound

            // Pick the entity that matches the response's primary id, falling
            // back to the first entity, to surface title/artist/artwork.
            val entity = dto.entitiesByUniqueId[dto.entityUniqueId]
                ?: dto.entitiesByUniqueId.values.firstOrNull()

            val song = Song(
                title = entity?.title,
                artist = entity?.artistName,
                artworkUrl = entity?.thumbnailUrl,
            )

            ResolveResult.Success(
                ResolvedSong(song = song, sourcePlatform = sourcePlatform, links = links),
            )
        } catch (e: HttpException) {
            when (e.code()) {
                429 -> ResolveResult.RateLimited
                404 -> ResolveResult.NotFound
                else -> ResolveResult.Error(e)
            }
        } catch (e: IOException) {
            ResolveResult.NoNetwork
        } catch (e: Exception) {
            ResolveResult.Error(e)
        }
    }
}
