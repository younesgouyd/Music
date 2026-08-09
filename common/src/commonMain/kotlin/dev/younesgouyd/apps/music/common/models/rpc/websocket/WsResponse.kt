package dev.younesgouyd.apps.music.common.models.rpc.websocket

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class WsResponse<T>(
    val correlationId: Uuid,
    val data: T
)