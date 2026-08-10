package dev.younesgouyd.apps.music.common.models.spotify

import dev.younesgouyd.apps.music.common.models.spotify.common.ArtistId
import dev.younesgouyd.apps.music.common.models.spotify.common.ExternalUrls
import dev.younesgouyd.apps.music.common.models.spotify.common.ImageObject
import dev.younesgouyd.apps.music.common.models.spotify.common.SpotifyUri
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