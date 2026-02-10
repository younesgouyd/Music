package dev.younesgouyd.libs.music.spotifyapi.models

import dev.younesgouyd.libs.music.spotifyapi.models.common.ArtistId
import dev.younesgouyd.libs.music.spotifyapi.models.common.ExternalUrls
import dev.younesgouyd.libs.music.spotifyapi.models.common.ImageObject
import dev.younesgouyd.libs.music.spotifyapi.models.common.SpotifyUri
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GET /artists/{id}
 */
@Serializable
data class Artist(
    @SerialName("external_urls")
    val externalUrls: ExternalUrls? = null,
    val href: String? = null,
    val id: ArtistId,
    val images: List<ImageObject>? = null,
    val name: String? = null,
    val type: String? = null,
    val uri: SpotifyUri? = null
)