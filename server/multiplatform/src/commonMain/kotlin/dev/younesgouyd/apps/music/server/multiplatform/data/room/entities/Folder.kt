package dev.younesgouyd.apps.music.server.multiplatform.data.room.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import dev.younesgouyd.apps.music.common.models.FolderId

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
