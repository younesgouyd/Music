package dev.younesgouyd.apps.music.common.models

import kotlinx.serialization.Serializable

@Serializable
sealed class QueueItemParameter {
    @Serializable data class Track(val id: TrackId) : QueueItemParameter()
    @Serializable data class Playlist(val id: PlaylistId) : QueueItemParameter()
    @Serializable data class Folder(val id: FolderId) : QueueItemParameter()
    @Serializable data class Artist(val id: SpotifyArtistId) : QueueItemParameter()
    @Serializable data class Album(val id: SpotifyAlbumId) : QueueItemParameter()
}