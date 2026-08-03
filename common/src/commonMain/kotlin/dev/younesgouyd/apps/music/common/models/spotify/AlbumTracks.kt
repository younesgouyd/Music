package dev.younesgouyd.apps.music.common.models.spotify

import dev.younesgouyd.apps.music.common.models.spotify.common.TrackId
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
