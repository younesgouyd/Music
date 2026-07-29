package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.components.util.DbOrder
import dev.younesgouyd.apps.music.common.ImportSessionId
import dev.younesgouyd.apps.music.common.ImportSessionItem
import dev.younesgouyd.apps.music.common.ImportSessionItemId
import io.ktor.client.*
import kotlinx.coroutines.flow.Flow

class ImportSessionItemRepo(
    private val client: HttpClient
) {
    fun get(id: ImportSessionItemId): Flow<ImportSessionItem?> {
        TODO()
//        return dao.get(id)
    }

    fun search(
        importSessionId: ImportSessionId,
        state: ImportSessionItem.State,
        titleQuery: String,
        order: DbOrder
    ): Flow<List<ImportSessionItem>> {
        TODO()
//        return dao.search(importSessionId, state, titleQuery.toSearchQuery(), order)
    }

    suspend fun updateState(
        id: ImportSessionItemId,
        state: ImportSessionItem.State
    ) {
        TODO()
//        dao.update(
//            state = state,
//            updateDatetime = System.currentTimeMillis(),
//            id = id
//        )
    }
}