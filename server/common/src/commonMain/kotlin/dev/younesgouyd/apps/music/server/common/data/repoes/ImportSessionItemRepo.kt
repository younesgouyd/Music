package dev.younesgouyd.apps.music.server.common.data.repoes

import dev.younesgouyd.apps.music.common.models.DbOrder
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import dev.younesgouyd.apps.music.common.models.MediaFileId
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
        return dao.getOldest(dev.younesgouyd.apps.music.common.models.ImportSessionItem.State.Pending)
    }

    fun search(
        importSessionId: ImportSessionId,
        state: dev.younesgouyd.apps.music.common.models.ImportSessionItem.State,
        titleQuery: String,
        order: DbOrder
    ): Flow<List<ImportSessionItem>> {
        return dao.search(importSessionId, state, titleQuery.toSearchQuery(), order)
    }

    suspend fun updateState(
        id: ImportSessionItemId,
        state: dev.younesgouyd.apps.music.common.models.ImportSessionItem.State,
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
        state: dev.younesgouyd.apps.music.common.models.ImportSessionItem.State
    ) {
        dao.update(
            state = state,
            updateDatetime = System.currentTimeMillis(),
            id = id
        )
    }
}