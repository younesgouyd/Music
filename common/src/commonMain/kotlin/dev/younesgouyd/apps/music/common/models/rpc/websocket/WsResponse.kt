package dev.younesgouyd.apps.music.common.models.rpc.websocket

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.uuid.Uuid

@Serializable
sealed class WsResponse {
    abstract val correlationId: Uuid

    @Serializable
    data class Done(
        override val correlationId: Uuid
    ) : WsResponse()

    @Serializable
    data class Data(
        override val correlationId: Uuid,
        val value: JsonElement
    ) : WsResponse()

    @Serializable
    data class Failure(
        override val correlationId: Uuid
    ) : WsResponse()
}
