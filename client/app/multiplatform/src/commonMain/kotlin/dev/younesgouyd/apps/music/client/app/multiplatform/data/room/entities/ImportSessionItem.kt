package dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.younesgouyd.apps.music.client.app.multiplatform.data.ImportSessionId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.MediaFileId
import dev.younesgouyd.apps.music.common.Inspection

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
    val state: State,
    val title: String,
    val durationMilliseconds: Long,
    val album: String?,
    val inspection: Inspection.ItemInspection,
    val localFilePath: String?,
    val albumTrackNumber: Int?,
    val lyrics: String?,
    val year: Int?,
    val imgId: MediaFileId?,
    val audioFileId: MediaFileId?,
    val creationDatetime: Long,
    val updateDatetime: Long
) {
    enum class State {
        Nonselected,
        Pending,
        InProgress,
        Completed,
        Cancelled,
        Failed
    }
}