package com.switchtune.app.data.odesli.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Subset of the Odesli `/v1-alpha.1/links` response that SwitchTune needs.
 * Unknown fields are ignored (configured on the Json instance).
 *
 * Docs: https://www.notion.so/Public-API-d8093b1bb8874f8b85527d985c4f9e68 (Odesli)
 */
@Serializable
data class OdesliResponseDto(
    @SerialName("entityUniqueId") val entityUniqueId: String? = null,
    @SerialName("pageUrl") val pageUrl: String? = null,
    @SerialName("entitiesByUniqueId") val entitiesByUniqueId: Map<String, EntityDto> = emptyMap(),
    @SerialName("linksByPlatform") val linksByPlatform: Map<String, PlatformLinkDto> = emptyMap(),
)

@Serializable
data class EntityDto(
    @SerialName("id") val id: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("artistName") val artistName: String? = null,
    @SerialName("thumbnailUrl") val thumbnailUrl: String? = null,
    @SerialName("apiProvider") val apiProvider: String? = null,
)

@Serializable
data class PlatformLinkDto(
    @SerialName("url") val url: String? = null,
    @SerialName("nativeAppUriMobile") val nativeAppUriMobile: String? = null,
    @SerialName("entityUniqueId") val entityUniqueId: String? = null,
)
