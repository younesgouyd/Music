package dev.younesgouyd.apps.music.server.common.data.repoes

import dev.younesgouyd.apps.music.common.ImportSessionId
import dev.younesgouyd.apps.music.common.ImportSessionItemId
import dev.younesgouyd.apps.music.common.MediaFileId
import dev.younesgouyd.apps.music.server.common.data.DbOrder
import dev.younesgouyd.apps.music.server.common.data.room.daos.ImportSessionItemDao
import dev.younesgouyd.apps.music.server.common.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.server.common.data.room.toSearchQuery
import kotlinx.coroutines.flow.Flow

class ImportSessionItemRepo(
    private val dao: ImportSessionItemDao
) {
    fun get(id: ImportSessionItemId): Flow<ImportSessionItem?> {
        return dao.get(id)
    }

    fun getOldestPending(): Flow<ImportSessionItem?> {
        return dao.getOldest(ImportSessionItem.State.Pending)
    }

    fun search(
        importSessionId: ImportSessionId,
        state: ImportSessionItem.State,
        titleQuery: String,
        order: DbOrder
    ): Flow<List<ImportSessionItem>> {
        return dao.search(importSessionId, state, titleQuery.toSearchQuery(), order)
    }

    suspend fun updateState(
        id: ImportSessionItemId,
        state: ImportSessionItem.State,
        audioFileId: MediaFileId?
    ) {
        dao.update(
            state = state,
            audioFileId = audioFileId,
            updateDatetime = System.currentTimeMillis(),
            id = id
        )
    }

    suspend fun updateState(
        id: ImportSessionItemId,
        state: ImportSessionItem.State
    ) {
        dao.update(
            state = state,
            updateDatetime = System.currentTimeMillis(),
            id = id
        )
    }
}