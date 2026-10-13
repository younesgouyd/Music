package dev.younesgouyd.apps.music.common.models.playback

import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.DeviceType
import kotlinx.serialization.Serializable

@Serializable
data class Client(
    val id: ClientId,
    val name: String,
    val deviceType: DeviceType?
)