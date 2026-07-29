package dev.younesgouyd.apps.music.common

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: TrackId,
    val importSessionItemId: ImportSessionItemId,
    val spotifyTrackId: SpotifyTrackId?,
    val folderId: FolderId?,
    val creationDatetime: Long,
    val updateDatetime: Long
)
