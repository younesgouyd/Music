package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.Offset
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.datamodels.ImportSession
import kotlinx.coroutines.flow.Flow

class ImportSessionRepo(
    private val server: Server
) {
    suspend fun getAll(limit: Int, offset: Offset.Index): List<ImportSession> {
        return server.get(
            Rpc.ToServer.ImportSession.GetAll(
                limit = limit,
                offset = offset
            )
        )
    }

    fun get(id: ImportSessionId): Flow<ImportSession?> {
        return server.observe(Rpc.ToServer.ImportSession.Get(id))
    }
}