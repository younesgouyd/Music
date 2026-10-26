package dev.younesgouyd.apps.music.common.models.datamodels

import dev.younesgouyd.apps.music.common.models.MediaFileId
import kotlinx.serialization.Serializable

@Serializable
data class PlaybackInterval(
    val audioFileId: MediaFileId,
    val context: PlaybackContext.ClientSent,
    val from: Long,
    val to: Long,
    val startDatetime: Long,
    val endDatetime: Long
)