package dev.younesgouyd.apps.music.server.multiplatform.data.room.entities

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.datamodels.ImportSessionItem
import dev.younesgouyd.apps.music.common.models.datamodels.Inspection

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ImportSession::class,
            parentColumns = ["id"],
            childColumns = ["importSessionId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT // TODO
        ),
        ForeignKey(
            entity = MediaFile::class,
            parentColumns = ["id"],
            childColumns = ["imgId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = MediaFile::class,
            parentColumns = ["id"],
            childColumns = ["audioFileId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["importSessionId"]),
        Index(value = ["imgId"], unique = true),
        Index(value = ["audioFileId"], unique = true)
    ]
)
data class ImportSessionItem(
    @PrimaryKey(autoGenerate = true)
    val id: ImportSessionItemId,
    val uri: String,
    val importSessionId: ImportSessionId,
    val state: ImportSessionItem.State,
    val title: String,
    val durationMilliseconds: Long,
    val album: String?,
    val inspection: Inspection.Item,
    val imgId: MediaFileId?,
    val audioFileId: MediaFileId?,
    val creationDatetime: Long,
    val updateDatetime: Long
)