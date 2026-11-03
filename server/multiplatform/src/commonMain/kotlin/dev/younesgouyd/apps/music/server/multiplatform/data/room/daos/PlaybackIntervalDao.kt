package dev.younesgouyd.apps.music.server.multiplatform.data.room.daos

import androidx.room3.Dao
import androidx.room3.Query
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.PlaybackInterval
import kotlin.uuid.Uuid

@Dao
abstract class PlaybackIntervalDao {
    @Query("""
        insert into PlaybackInterval (uuid, audioFileId, possibleDuplicateOf, clientId, importId, trackId, playlistId, folderId, albumId, artistId, `from`, `to`, estimatedTo, websocketPingPeriodMs, websocketPingTimeoutMs, startDatetime, endDatetime, creationDatetime, updateDatetime)
        values (:uuid, :audioFileId, :possibleDuplicateOf, :clientId, :importId, :trackId, :playlistId, :folderId, :albumId, :artistId, :from, :to, :estimatedTo, :websocketPingPeriodMs, :websocketPingTimeoutMs, :startDatetime, :endDatetime, :creationDatetime, :updateDatetime)
    """)
    abstract suspend fun add(
        uuid: Uuid,
        audioFileId: MediaFileId,
        possibleDuplicateOf: PlaybackIntervalId?,
        clientId: ClientId,
        importId: ImportSessionItemId,
        trackId: TrackId,
        playlistId: PlaylistId?,
        folderId: FolderId?,
        albumId: SpotifyAlbumId?,
        artistId: SpotifyArtistId?,
        from: Long,
        to: Long?,
        estimatedTo: Long?,
        websocketPingPeriodMs: Long,
        websocketPingTimeoutMs: Long,
        startDatetime: Long,
        endDatetime: Long,
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Query("select * from PlaybackInterval where uuid = :uuid")
    abstract suspend fun getByUuid(uuid: Uuid): PlaybackInterval?

    @Query("""
        update playbackinterval
        set `to` = :to,
            endDatetime = :endDatetime,
            updateDatetime = :updateDatetime
        where uuid = :uuid
    """)
    abstract suspend fun update(
        to: Long?,
        endDatetime: Long,
        updateDatetime: Long,
        uuid: Uuid
    )
}