package dev.younesgouyd.apps.music.common.models.datamodels

import dev.younesgouyd.apps.music.common.models.MediaFileId
import kotlinx.serialization.Serializable

@Serializable
data class MediaFile(
    val id: MediaFileId,
    val fileName: String?,
    val creationDatetime: Long
)