package dev.younesgouyd.apps.music.common.models.datamodels

import dev.younesgouyd.apps.music.common.models.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class QueueItem(
    var key: Int? = null,
    val id: TrackId,
    val importId: ImportSessionItemId,
    val name: String,
    val album: Album,
    val imageId: MediaFileId?,
    val artists: Artists,
    val audioId: MediaFileId?,
    val durationMs: Long?,
) {
    @Transient
    var addedFrom: AddedFrom? = null
        private set

    constructor(
        key: Int? = null,
        id: TrackId,
        importId: ImportSessionItemId,
        name: String,
        album: Album,
        imageId: MediaFileId?,
        artists: Artists,
        audioId: MediaFileId?,
        durationMs: Long?,
        addedFrom: AddedFrom
    ) : this(key, id, importId, name, album, imageId, artists, audioId, durationMs) {
        this.addedFrom = addedFrom
    }

    @Serializable
    sealed class Album {
        data class SpotifyAlbum(
            val id: SpotifyAlbumId,
            val name: String
        ) : Album()

        @Serializable
        data class ImportAlbum(
            val name: String?
        ) : Album()
    }

    @Serializable
    sealed class Artists {
        @Serializable
        data class SpotifyArtists(
            val list: List<Pair<SpotifyArtistId, String>>
        ) : Artists()

        @Serializable
        data class ImportArtist(
            val list: List<String>
        ) : Artists()
    }

    sealed class AddedFrom {
        data object None : AddedFrom()
        data class Playlist(val id: PlaylistId) : AddedFrom()
        data class Folder(val id: FolderId) : AddedFrom()
        data class Album(val id: SpotifyAlbumId) : AddedFrom()
        data class Artist(val id: SpotifyArtistId) : AddedFrom()
    }
}