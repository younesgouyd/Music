package dev.younesgouyd.apps.music.common.models.datamodels

import dev.younesgouyd.apps.music.common.models.MediaFileId
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class PlaybackInterval(
    val uuid: Uuid,
    val audioFileId: MediaFileId,
    val context: PlaybackContext.ClientSent,
    val from: Long,
    val to: Long,
    val startDatetime: Long,
    val endDatetime: Long
)