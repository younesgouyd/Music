package dev.younesgouyd.apps.music.common.models.datamodels

import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import dev.younesgouyd.apps.music.common.models.TrackId
import kotlinx.serialization.Serializable

@Serializable
data class QueueItem(
    var key: Int? = null,
    val id: TrackId,
    val name: String,
    val album: Album,
    val imageId: MediaFileId?,
    val artists: Artists,
    val audioId: MediaFileId?,
    val durationMs: Long?
) {
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
}