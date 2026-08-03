package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.DbOrder
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.ImportSessionItem
import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import dev.younesgouyd.apps.music.common.models.rpc.ImportSessionItemRpc
import io.ktor.client.call.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ImportSessionItemRepo(
    private val backend: Backend
) {
    fun get(id: ImportSessionItemId): Flow<ImportSessionItem?> {
        return flow {
            emit(
                backend.call(ImportSessionItemRpc.Get(id)).body<ImportSessionItem?>()
            )
        }
    }

    fun search(
        importSessionId: ImportSessionId,
        state: ImportSessionItem.State,
        titleQuery: String,
        order: DbOrder
    ): Flow<List<ImportSessionItem>> {
        return flow {
            emit(
                backend.call(
                    ImportSessionItemRpc.Search(
                        importSessionId = importSessionId,
                        state = state,
                        titleQuery = titleQuery,
                        order = order
                    )
                ).body<List<ImportSessionItem>>()
            )
        }
    }

    suspend fun updateState(
        id: ImportSessionItemId,
        state: ImportSessionItem.State
    ) {
        backend.call(
            ImportSessionItemRpc.UpdateState(
                id = id,
                state = state
            )
        )
    }
}