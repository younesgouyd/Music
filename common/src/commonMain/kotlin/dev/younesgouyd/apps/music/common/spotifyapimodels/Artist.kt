package dev.younesgouyd.apps.music.common.spotifyapimodels

import dev.younesgouyd.apps.music.common.spotifyapimodels.common.ArtistId
import dev.younesgouyd.apps.music.common.spotifyapimodels.common.ExternalUrls
import dev.younesgouyd.apps.music.common.spotifyapimodels.common.ImageObject
import dev.younesgouyd.apps.music.common.spotifyapimodels.common.SpotifyUri
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