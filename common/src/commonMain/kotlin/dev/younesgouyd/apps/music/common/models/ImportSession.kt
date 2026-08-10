package dev.younesgouyd.apps.music.common.models

import kotlinx.serialization.Serializable

@Serializable
data class ImportSession(
    val id: ImportSessionId,
    val uri: String,
    val inspection: Inspection.Container,
    val destinationFolderId: FolderId?,
    val imgId: MediaFileId?,
    val creationDatetime: Long
)