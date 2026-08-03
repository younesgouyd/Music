package dev.younesgouyd.apps.music.server.common.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.ImportSessionItem

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