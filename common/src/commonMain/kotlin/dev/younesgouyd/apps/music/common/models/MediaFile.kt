package dev.younesgouyd.apps.music.common.models

import kotlinx.serialization.Serializable

@Serializable
data class MediaFile(
    val id: MediaFileId,
    val fileName: String?,
    val creationDatetime: Long
)