package dev.younesgouyd.apps.music.common.models.playback

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class Client(
    val id: Uuid
)