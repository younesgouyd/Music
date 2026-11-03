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
            entity = MediaFile::class,
            parentColumns = ["id"],
            childColumns = ["audioFileId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = PlaybackInterval::class,
            parentColumns = ["id"],
            childColumns = ["possibleDuplicateOf"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = ImportSessionItem::class,
            parentColumns = ["id"],
            childColumns = ["importId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = Track::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = SpotifyAlbum::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = SpotifyArtist::class,
            parentColumns = ["id"],
            childColumns = ["artistId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.NO_ACTION
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