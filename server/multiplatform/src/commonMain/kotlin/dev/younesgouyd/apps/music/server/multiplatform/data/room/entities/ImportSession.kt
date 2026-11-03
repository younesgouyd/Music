package dev.younesgouyd.apps.music.server.multiplatform.data.room.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.datamodels.Inspection

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