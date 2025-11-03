package dev.younesgouyd.apps.music.common.data.repoes

import dev.younesgouyd.apps.music.common.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.common.data.room.entities.ImportSessionItemDao
import kotlinx.coroutines.flow.Flow

class ImportSessionItemRepo(
    private val dao: ImportSessionItemDao
) {
    fun get(id: Long): Flow<ImportSessionItem> {
        return dao.get(id)
    }

    fun getOldestPending(): Flow<ImportSessionItem?> {
        return dao.getOldest(ImportSessionItem.State.Pending)
    }

    fun getImportSessionItems(importSessionId: Long, state: List<ImportSessionItem.State>): Flow<List<ImportSessionItem>> {
        return dao.getImportSessionItems(importSessionId, state)
    }

    suspend fun updateState(id: Long, state: ImportSessionItem.State) {
        dao.updateState(
            state = state,
            updateDatetime = System.currentTimeMillis(),
            id = id
        )
    }
}