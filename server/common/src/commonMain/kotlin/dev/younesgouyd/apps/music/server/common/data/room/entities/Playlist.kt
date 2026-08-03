package dev.younesgouyd.apps.music.server.common.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.PlaylistId

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["folderId"])
    ]
)
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: PlaylistId,
    val name: String,
    val folderId: FolderId?,
    val creationDatetime: Long,
    val updateDatetime: Long
)