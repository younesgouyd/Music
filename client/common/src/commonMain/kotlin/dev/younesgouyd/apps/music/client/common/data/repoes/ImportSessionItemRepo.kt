package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Server
import dev.younesgouyd.apps.music.common.models.DbOrder
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.ImportSessionItem
import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import dev.younesgouyd.apps.music.common.models.rpc.ImportSessionItemRpc
import kotlinx.coroutines.flow.Flow

class ImportSessionItemRepo(
    private val server: Server
) {
    fun get(id: ImportSessionItemId): Flow<ImportSessionItem?> {
        return server.stream(ImportSessionItemRpc.Get(id))
    }

    fun search(
        importSessionId: ImportSessionId,
        state: ImportSessionItem.State,
        titleQuery: String,
        order: DbOrder
    ): Flow<List<ImportSessionItem>> {
        return server.stream(
            ImportSessionItemRpc.Search(
                importSessionId = importSessionId,
                state = state,
                titleQuery = titleQuery,
                order = order
            )
        )
    }

    suspend fun updateState(
        id: ImportSessionItemId,
        state: ImportSessionItem.State
    ) {
        server.call(
            ImportSessionItemRpc.UpdateState(
                id = id,
                state = state
            )
        )
    }
}