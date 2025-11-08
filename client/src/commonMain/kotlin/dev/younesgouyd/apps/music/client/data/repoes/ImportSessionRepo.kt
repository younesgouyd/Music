package dev.younesgouyd.apps.music.client.data.repoes

import kotlinx.coroutines.flow.Flow

class ImportSessionRepo(
    private val dao: dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionDao
) {
    fun getAll(): Flow<List<dev.younesgouyd.apps.music.client.data.room.entities.ImportSession>> {
        return dao.getAll()
    }

    fun get(id: Long): Flow<dev.younesgouyd.apps.music.client.data.room.entities.ImportSession> {
        return dao.get(id)
    }
}