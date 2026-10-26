package dev.younesgouyd.apps.music.server.multiplatform.data.room.daos

import androidx.room.Dao
import androidx.room.Query
import dev.younesgouyd.apps.music.common.models.*

@Dao
abstract class PlaybackIntervalDao {
    @Query("""
            insert into PlaybackInterval (audioFileId, clientId, importId, trackId, playlistId, folderId, albumId, artistId, `from`, `to`, startDatetime, endDatetime, creationDatetime)
            values (:audioFileId, :clientId, :importId, :trackId, :playlistId, :folderId, :albumId, :artistId, :from, :to, :startDatetime, :endDatetime, :creationDatetime)
    """)
    abstract suspend fun add(
        audioFileId: MediaFileId,
        clientId: ClientId,
        importId: ImportSessionItemId,
        trackId: TrackId,
        playlistId: PlaylistId?,
        folderId: FolderId?,
        albumId: SpotifyAlbumId?,
        artistId: SpotifyArtistId?,
        from: Long,
        to: Long,
        startDatetime: Long,
        endDatetime: Long,
        creationDatetime: Long
    )
}