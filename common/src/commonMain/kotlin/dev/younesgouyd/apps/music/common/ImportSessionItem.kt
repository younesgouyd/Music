package dev.younesgouyd.apps.music.common

import kotlinx.serialization.Serializable

@Serializable
data class ImportSessionItem(
    val id: ImportSessionItemId,
    val uri: String,
    val importSessionId: ImportSessionId,
    val state: State,
    val title: String,
    val durationMilliseconds: Long,
    val album: String?,
    val inspection: Inspection.Item,
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