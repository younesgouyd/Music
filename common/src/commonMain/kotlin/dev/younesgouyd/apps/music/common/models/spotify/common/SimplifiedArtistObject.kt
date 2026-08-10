package dev.younesgouyd.apps.music.common.models.spotify.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SimplifiedArtistObject(
    @SerialName("external_urls")
    val externalUrls: ExternalUrls? = null,
    val href: String? = null,
    val id: ArtistId,
    val name: String,
    val type: String? = null,
    val uri: SpotifyUri
)