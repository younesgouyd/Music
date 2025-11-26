package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.ImportSessionId
import dev.younesgouyd.apps.music.client.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItemDao
import dev.younesgouyd.apps.music.client.data.room.toSearchQuery
import kotlinx.coroutines.flow.Flow

class ImportSessionItemRepo(
    private val dao: ImportSessionItemDao
) {
    fun getAll(): Flow<List<ImportSessionItem>> {
        return dao.getAll()
    }

    fun get(id: ImportSessionItemId): Flow<ImportSessionItem> {
        return dao.get(id)
    }

    fun getOldestPending(): Flow<ImportSessionItem?> {
        return dao.getOldest(ImportSessionItem.State.Pending)
    }

    fun search(
        importSessionId: ImportSessionId,
        state: ImportSessionItem.State,
        titleQuery: String
    ): Flow<List<ImportSessionItem>> {
        return dao.search(importSessionId, state, titleQuery.toSearchQuery())
    }

    suspend fun updateState(
        id: ImportSessionItemId,
        state: ImportSessionItem.State
    ) {
        dao.updateState(
            state = state,
            updateDatetime = System.currentTimeMillis(),
            id = id
        )
    }
}