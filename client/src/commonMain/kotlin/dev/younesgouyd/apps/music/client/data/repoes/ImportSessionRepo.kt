package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.room.entities.ImportSession
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionDao
import kotlinx.coroutines.flow.Flow

class ImportSessionRepo(
    private val dao: ImportSessionDao
) {
    fun getAll(): Flow<List<ImportSession>> {
        return dao.getAll()
    }

    fun get(id: Long): Flow<ImportSession> {
        return dao.get(id)
    }
}