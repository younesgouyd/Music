package dev.younesgouyd.apps.music.common.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.uuid.Uuid

@Serializable
sealed class WSMessage {
    @Serializable
    class Registration(
        val value: ClientId
    ) : WSMessage()

    @Serializable
    sealed class Request : WSMessage() {
        abstract val correlationId: Uuid

        @Serializable
        data class Execute(
            override val correlationId: Uuid,
            val rpc: Rpc
        ) : Request()

        @Serializable
        data class Cancel(
            override val correlationId: Uuid
        ) : Request()
    }

    @Serializable
    sealed class Response : WSMessage() {
        abstract val correlationId: Uuid

        @Serializable
        data class Done(
            override val correlationId: Uuid
        ) : Response()

        @Serializable
        data class Data(
            override val correlationId: Uuid,
            val value: JsonElement
        ) : Response()
    }
}