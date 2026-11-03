package dev.younesgouyd.apps.music.server.multiplatform.data.room.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import dev.younesgouyd.apps.music.common.models.MediaFileId

@Entity
data class MediaFile(
    @PrimaryKey(autoGenerate = true)
    val id: MediaFileId,
    val fileName: String?,
    val creationDatetime: Long
)