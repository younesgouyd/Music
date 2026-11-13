package dev.younesgouyd.apps.music.server.multiplatform.data.room.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import dev.younesgouyd.apps.music.common.models.*
import kotlin.uuid.Uuid

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = PlaybackInterval::class,
            parentColumns = ["id"],
            childColumns = ["possibleDuplicateOf"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ImportSessionItem::class,
            parentColumns = ["id"],
            childColumns = ["importId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["uuid"], unique = true)
    ]
)
data class PlaybackInterval(
    @PrimaryKey(autoGenerate = true)
    val id: PlaybackIntervalId,
    val uuid: Uuid,
    val audioFileId: MediaFileId,
    val possibleDuplicateOf: PlaybackIntervalId?,

    // context
    val clientId: ClientId,
    val importId: ImportSessionItemId,
    val trackId: TrackId,
    val playlistId: PlaylistId?,
    val folderId: FolderId?,
    val albumId: SpotifyAlbumId?,
    val artistId: SpotifyArtistId?,
    // ----------------------

    val from: Long,
    val to: Long?,
    val estimatedTo: Long?,
    val websocketPingPeriodMs: Long,
    val websocketPingTimeoutMs: Long,
    val startDatetime: Long,
    val endDatetime: Long,
    val creationDatetime: Long,
    val updateDatetime: Long
)