package dev.younesgouyd.apps.music.common.models.datamodels

import dev.younesgouyd.apps.music.common.models.*
import kotlinx.serialization.Serializable

@Serializable
data class PlaybackContext(
    val clientId: ClientId,
    val importId: ImportSessionItemId,
    val trackId: TrackId,
    val playlistId: PlaylistId?,
    val folderId: FolderId?,
    val albumId: SpotifyAlbumId?,
    val artistId: SpotifyArtistId?
) {
    constructor(queueItem: QueueItem, clientId: ClientId) : this(
        clientId = clientId,
        importId = queueItem.importId,
        trackId = queueItem.id,
        playlistId = (queueItem.addedFrom as? QueueItem.AddedFrom.Playlist)?.id,
        folderId = (queueItem.addedFrom as? QueueItem.AddedFrom.Folder)?.id,
        albumId = (queueItem.addedFrom as? QueueItem.AddedFrom.Album)?.id,
        artistId = (queueItem.addedFrom as? QueueItem.AddedFrom.Artist)?.id
    )
}