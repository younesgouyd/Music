package dev.younesgouyd.apps.music.common.models.rpc

import kotlinx.serialization.Serializable

@Serializable
sealed class InspectionRpc : Rpc.ToServer() {
    @Serializable
    data class Inspect(
        val url: String
    ) : InspectionRpc()
}