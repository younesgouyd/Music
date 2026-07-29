package dev.younesgouyd.apps.music.common

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistTrack(
    val track: Track,
    val playlistTrackCrossRefId: PlaylistTrackCrossRefId,
    val originalImport: ImportSessionItem,
    val spotifyTrack: SpotifyTrack?,
    val playlistCrossRef: PlaylistTrackCrossRef?
)