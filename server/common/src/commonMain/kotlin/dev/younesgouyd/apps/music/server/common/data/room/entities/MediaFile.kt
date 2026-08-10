package dev.younesgouyd.apps.music.server.common.data.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.younesgouyd.apps.music.common.models.MediaFileId

@Entity
data class MediaFile(
    @PrimaryKey(autoGenerate = true)
    val id: MediaFileId,
    val fileName: String?,
    val creationDatetime: Long
)