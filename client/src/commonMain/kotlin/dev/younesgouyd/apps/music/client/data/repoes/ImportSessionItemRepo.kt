package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.room.toSearchQuery
import kotlinx.coroutines.flow.Flow

class ImportSessionItemRepo(
    private val dao: dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItemDao
) {
    fun get(id: Long): Flow<dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem> {
        return dao.get(id)
    }

    fun getOldestPending(): Flow<dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem?> {
        return dao.getOldest(dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem.State.Pending)
    }

    fun search(
        importSessionId: Long,
        state: dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem.State,
        titleQuery: String
    ): Flow<List<dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem>> {
        return dao.search(importSessionId, state, titleQuery.toSearchQuery())
    }

    suspend fun updateState(
        id: Long,
        state: dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem.State
    ) {
        dao.updateState(
            state = state,
            updateDatetime = System.currentTimeMillis(),
            id = id
        )
    }
}