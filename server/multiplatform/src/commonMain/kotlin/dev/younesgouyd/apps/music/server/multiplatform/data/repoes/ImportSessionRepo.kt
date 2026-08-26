package dev.younesgouyd.apps.music.server.multiplatform.data.repoes

import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.Offset
import dev.younesgouyd.apps.music.server.multiplatform.data.room.daos.ImportSessionDao
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.ImportSession
import kotlinx.coroutines.flow.Flow

class ImportSessionRepo(
    private val dao: ImportSessionDao
) {
    suspend fun getAll(limit: Int, offset: Offset.Index): List<ImportSession> {
        return dao.getAll(limit, offset.value)
    }

    fun get(id: ImportSessionId): Flow<ImportSession?> {
        return dao.get(id)
    }
}