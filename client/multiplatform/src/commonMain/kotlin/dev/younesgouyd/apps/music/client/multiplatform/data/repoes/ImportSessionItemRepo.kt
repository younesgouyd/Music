package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.DbOrder
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.datamodels.ImportSessionItem
import kotlinx.coroutines.flow.Flow

class ImportSessionItemRepo(
    private val server: Server
) {
    fun get(id: ImportSessionItemId): Flow<ImportSessionItem?> {
        return server.observe(Rpc.ToServer.ImportSessionItem.Get(id))
    }

    fun search(
        importSessionId: ImportSessionId,
        state: ImportSessionItem.State,
        titleQuery: String,
        order: DbOrder
    ): Flow<List<ImportSessionItem>> {
        return server.observe(
            Rpc.ToServer.ImportSessionItem.Search(
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
            Rpc.ToServer.ImportSessionItem.UpdateState(
                id = id,
                state = state
            )
        )
    }
}