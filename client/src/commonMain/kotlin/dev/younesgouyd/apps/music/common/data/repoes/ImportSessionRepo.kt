package dev.younesgouyd.apps.music.common.data.repoes

import dev.younesgouyd.apps.music.common.data.room.entities.ImportSessionDao
import dev.younesgouyd.apps.music.common.data.room.entities.ImportSessionWithItems
import dev.younesgouyd.apps.music.common.util.ImportSessionState
import dev.younesgouyd.apps.music.common.util.ImportSourceType
import kotlinx.coroutines.flow.Flow

class ImportSessionRepo(
    private val dao: ImportSessionDao,
) {
    fun getAll(): Flow<List<ImportSessionWithItems>> {
        return dao.getAll()
    }

    fun get(id: Long): Flow<ImportSessionWithItems> {
        return dao.get(id)
    }

    fun getOldestPending(): Flow<ImportSessionWithItems?> {
        return dao.getOldestPending()
    }

    fun getOngoingImportsCount(): Flow<Long> {
        return dao.getOngoingImportsCount()
    }

    suspend fun addLocalSession(uri: String) {
        require(uri.isNotBlank())
        val currentTime = System.currentTimeMillis()
        dao.add(
            uri = uri,
            sourceType = ImportSourceType.Local,
            state = ImportSessionState.Pending,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }

    suspend fun addUrlSession(uri: String, items: Map<Long, String>) {
        require(uri.isNotBlank() && items.isNotEmpty())
        return dao.addUrlSession(uri, items)
    }

    suspend fun updateState(id: Long, sessionState: ImportSessionState) {
        dao.updateState(sessionState, System.currentTimeMillis(), id)
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
    }
}
