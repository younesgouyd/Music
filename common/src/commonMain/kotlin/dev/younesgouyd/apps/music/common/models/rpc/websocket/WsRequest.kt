package dev.younesgouyd.apps.music.common.models.rpc.websocket

import dev.younesgouyd.apps.music.common.models.rpc.Rpc
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
sealed class WsRequest {
    abstract val correlationId: Uuid

    @Serializable
    data class Execute(
        override val correlationId: Uuid,
        val rpc: Rpc
    ) : WsRequest()

    @Serializable
    data class Cancel(
        override val correlationId: Uuid
    ) : WsRequest()
}