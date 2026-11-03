package dev.younesgouyd.apps.music.common.models.datamodels

import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.MediaFileId
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class PlaybackIntervalStart(
    val uuid: Uuid,
    val clientId: ClientId,
    val audioFileId: MediaFileId,
    val from: Long,
    val datetime: Long,
    val context: PlaybackContext.ClientSent
)