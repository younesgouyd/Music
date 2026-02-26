package dev.younesgouyd.libs.music.client.spotifyapi.models

import dev.younesgouyd.libs.music.client.spotifyapi.models.common.TrackId
import kotlinx.serialization.Serializable

/**
 * GET /albums/{id}/tracks
 */
@Serializable
data class AlbumTracks(
    val next: String?,
    val items: List<SimplifiedTrackObject>
) {
    @Serializable
    data class SimplifiedTrackObject(
        val id: TrackId
    )
}
