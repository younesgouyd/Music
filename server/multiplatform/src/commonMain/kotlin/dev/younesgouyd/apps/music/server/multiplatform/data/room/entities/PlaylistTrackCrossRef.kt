package dev.younesgouyd.apps.music.server.multiplatform.data.room.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import dev.younesgouyd.apps.music.common.models.PlaylistId
import dev.younesgouyd.apps.music.common.models.PlaylistTrackCrossRefId
import dev.younesgouyd.apps.music.common.models.TrackId

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Track::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId", "trackId"], unique = true),
        Index(value = ["playlistId", "position"], unique = true),
        Index(value = ["playlistId"]),
        Index(value = ["trackId"])
    ]
)
data class PlaylistTrackCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: PlaylistTrackCrossRefId,
    val playlistId: PlaylistId,
    val trackId: TrackId,
    val position: Int,
    val creationDatetime: Long,
    val updateDatetime: Long
)