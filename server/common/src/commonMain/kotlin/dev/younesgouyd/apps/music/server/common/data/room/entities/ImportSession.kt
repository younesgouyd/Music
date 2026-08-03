package dev.younesgouyd.apps.music.server.common.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.Inspection
import dev.younesgouyd.apps.music.common.models.MediaFileId

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["destinationFolderId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = MediaFile::class,
            parentColumns = ["id"],
            childColumns = ["imgId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["destinationFolderId"]),
        Index(value = ["imgId"], unique = true)
    ]
)
data class ImportSession(
    @PrimaryKey(autoGenerate = true)
    val id: ImportSessionId,
    val uri: String,
    val inspection: Inspection.Container,
    val destinationFolderId: FolderId?,
    val imgId: MediaFileId?,
    val creationDatetime: Long
)