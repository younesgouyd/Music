package dev.younesgouyd.apps.music.server.multiplatform.data.room.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
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