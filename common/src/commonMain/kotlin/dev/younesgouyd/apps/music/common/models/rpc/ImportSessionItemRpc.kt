package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.DbOrder
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.ImportSessionItem
import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import kotlinx.serialization.Serializable

@Serializable
sealed class ImportSessionItemRpc : Rpc() {
    @Serializable
    data class Get(
        val id: ImportSessionItemId
    ) : ImportSessionItemRpc()

    @Serializable
    data class Search(
        val importSessionId: ImportSessionId,
        val state: ImportSessionItem.State,
        val titleQuery: String,
        val order: DbOrder
    ) : ImportSessionItemRpc()

    @Serializable
    data class UpdateState(
        val id: ImportSessionItemId,
        val state: ImportSessionItem.State
    ) : ImportSessionItemRpc()
}