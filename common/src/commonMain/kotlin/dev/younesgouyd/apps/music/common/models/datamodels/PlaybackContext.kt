package dev.younesgouyd.apps.music.common.models.datamodels

import dev.younesgouyd.apps.music.common.models.*
import kotlinx.serialization.Serializable

@Serializable
sealed class PlaybackContext {
    @Serializable
    data class ServerSent(
        val importId: ImportSessionItemId,
        val trackId: TrackId,
        val playlistId: PlaylistId?,
        val folderId: FolderId?,
        val albumId: SpotifyAlbumId?,
        val artistId: SpotifyArtistId?
    ) : PlaybackContext() {
        constructor(queueItem: QueueItem) : this(
            importId = queueItem.importId,
            trackId = queueItem.id,
            playlistId = (queueItem.addedFrom as? QueueItem.AddedFrom.Playlist)?.id,
            folderId = (queueItem.addedFrom as? QueueItem.AddedFrom.Folder)?.id,
            albumId = (queueItem.addedFrom as? QueueItem.AddedFrom.Album)?.id,
            artistId = (queueItem.addedFrom as? QueueItem.AddedFrom.Artist)?.id
        )
    }

    @Serializable
    data class ClientSent(
        val clientId: ClientId,
        val importId: ImportSessionItemId,
        val trackId: TrackId,
        val playlistId: PlaylistId?,
        val folderId: FolderId?,
        val albumId: SpotifyAlbumId?,
        val artistId: SpotifyArtistId?
    ) : PlaybackContext()
}

fun PlaybackContext.ServerSent.toClientSent(clientId: ClientId): PlaybackContext.ClientSent {
    return PlaybackContext.ClientSent(
        clientId = clientId,
        importId = importId,
        trackId = trackId,
        playlistId = playlistId,
        folderId = folderId,
        albumId = albumId,
        artistId = artistId
    )
}