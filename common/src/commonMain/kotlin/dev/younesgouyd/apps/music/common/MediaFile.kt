package dev.younesgouyd.apps.music.common

import kotlinx.serialization.Serializable

@Serializable
data class MediaFile(
    val id: MediaFileId,
    val fileName: String?,
    val creationDatetime: Long
)