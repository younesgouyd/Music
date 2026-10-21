package dev.younesgouyd.apps.music.common.models.datamodels

import dev.younesgouyd.apps.music.common.models.FolderId
import kotlinx.serialization.Serializable

@Serializable
data class Folder(
    val id: FolderId,
    val name: String,
    val parentFolderId: FolderId?,
    val creationDatetime: Long,
    val updateDatetime: Long
)