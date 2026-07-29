package dev.younesgouyd.apps.music.common

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistTrackCrossRef(
    val id: PlaylistTrackCrossRefId,
    val playlistId: PlaylistId,
    val trackId: TrackId,
    val position: Int,
    val creationDatetime: Long,
    val updateDatetime: Long
)