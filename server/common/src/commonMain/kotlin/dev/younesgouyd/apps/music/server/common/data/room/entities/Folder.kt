package dev.younesgouyd.apps.music.server.common.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.younesgouyd.apps.music.common.FolderId

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["parentFolderId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parentFolderId"])
    ]
)
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: FolderId,
    val name: String,
    val parentFolderId: FolderId?,
    val creationDatetime: Long,
    val updateDatetime: Long
)
