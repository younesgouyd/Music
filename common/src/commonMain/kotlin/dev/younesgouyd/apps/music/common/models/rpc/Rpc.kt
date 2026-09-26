package dev.younesgouyd.apps.music.common.models.rpc

import kotlinx.serialization.Serializable

@Serializable
sealed class Rpc {
    @Serializable
    sealed class ToClient : Rpc()

    @Serializable
    sealed class ToServer : Rpc()
}