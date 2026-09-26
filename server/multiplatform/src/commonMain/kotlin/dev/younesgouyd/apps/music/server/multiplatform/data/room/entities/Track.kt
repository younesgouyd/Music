package dev.younesgouyd.apps.music.server.multiplatform.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackId
import dev.younesgouyd.apps.music.common.models.TrackId

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ImportSessionItem::class,
            parentColumns = ["id"],
            childColumns = ["importSessionItemId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = SpotifyTrack::class,
            parentColumns = ["id"],
            childColumns = ["spotifyTrackId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT // TODO: if a track is deleted, also delete its SpotifyTrack if unused elsewhere, and clear its imports if unused
        )
    ],
    indices = [
        Index(value = ["importSessionItemId"], unique = true),
        Index(value = ["spotifyTrackId"], unique = true),
        Index(value = ["folderId"])
    ]
)
data class Track(
    @PrimaryKey(autoGenerate = true)
    val id: TrackId,
    val importSessionItemId: ImportSessionItemId,
    val spotifyTrackId: SpotifyTrackId?,
    val folderId: FolderId?,
    val creationDatetime: Long,
    val updateDatetime: Long
)
