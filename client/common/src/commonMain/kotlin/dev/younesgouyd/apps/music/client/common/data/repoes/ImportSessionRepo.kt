package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.ImportSession
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.Offset
import dev.younesgouyd.apps.music.common.models.rpc.ImportSessionRpc
import kotlinx.coroutines.flow.Flow

class ImportSessionRepo(
    private val backend: Backend
) {
    suspend fun getAll(limit: Int, offset: Offset.Index): List<ImportSession> {
        return backend.callForResult(
            ImportSessionRpc.GetAll(
                limit = limit,
                offset = offset
            )
        )
    }

    fun get(id: ImportSessionId): Flow<ImportSession?> {
        return backend.stream(ImportSessionRpc.Get(id))
    }
}