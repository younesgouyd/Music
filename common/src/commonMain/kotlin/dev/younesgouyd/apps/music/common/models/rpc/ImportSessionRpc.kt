package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.Offset
import kotlinx.serialization.Serializable

@Serializable
sealed class ImportSessionRpc : Rpc() {
    @Serializable
    data class GetAll(
        val limit: Int,
        val offset: Offset.Index
    ) : ImportSessionRpc()

    @Serializable
    data class Get(
        val id: ImportSessionId
    ) : ImportSessionRpc()
}