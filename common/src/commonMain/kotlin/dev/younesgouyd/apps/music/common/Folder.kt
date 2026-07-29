package dev.younesgouyd.apps.music.common

import kotlinx.serialization.Serializable

@Serializable
data class Folder(
    val id: FolderId,
    val name: String,
    val parentFolderId: FolderId?,
    val creationDatetime: Long,
    val updateDatetime: Long
)
