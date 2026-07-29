package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.common.ImportSession
import dev.younesgouyd.apps.music.common.ImportSessionId
import dev.younesgouyd.apps.music.common.Offset
import io.ktor.client.*
import kotlinx.coroutines.flow.Flow

class ImportSessionRepo(
    private val client: HttpClient
) {
    suspend fun getAll(limit: Int, offset: Offset.Index): List<ImportSession> {
        TODO()
    }

    fun get(id: ImportSessionId): Flow<ImportSession?> {
        TODO()
    }
}