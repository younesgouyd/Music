package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.ImportSession
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.Offset
import dev.younesgouyd.apps.music.common.models.rpc.ImportSessionRpc
import io.ktor.client.call.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ImportSessionRepo(
    private val backend: Backend
) {
    suspend fun getAll(limit: Int, offset: Offset.Index): List<ImportSession> {
        return backend.call(
            ImportSessionRpc.GetAll(
                limit = limit,
                offset = offset
            )
        ).body<List<ImportSession>>()
    }

    fun get(id: ImportSessionId): Flow<ImportSession?> {
        return flow {
            emit(
                backend.call(ImportSessionRpc.Get(id)).body<ImportSession?>()
            )
        }
    }
}