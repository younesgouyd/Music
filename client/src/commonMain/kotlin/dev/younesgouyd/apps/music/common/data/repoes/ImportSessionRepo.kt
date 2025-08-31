package dev.younesgouyd.apps.music.common.data.repoes

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.younesgouyd.apps.music.common.data.sqldelight.migrations.Import_session
import dev.younesgouyd.apps.music.common.data.sqldelight.queries.ImportSessionQueries
import dev.younesgouyd.apps.music.common.util.ImportSessionState
import dev.younesgouyd.apps.music.common.util.ImportSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ImportSessionRepo(private val queries: ImportSessionQueries) {
    fun getAll(): Flow<List<Import_session>> {
        return queries.getAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun get(id: Long): Flow<Import_session> {
        return queries.get(id)
            .asFlow()
            .mapToOne(Dispatchers.IO)
    }

    fun getOldestPending(): Flow<Import_session?> {
        return queries.getOldestPending()
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
    }

    fun getOngoingImportsCount(): Flow<Long> {
        return queries.getOngoingImportsCount()
            .asFlow()
            .mapToOne(Dispatchers.IO)
    }

    suspend fun add(uri: String, importSourceType: ImportSourceType, domainName: String?, sessionState: ImportSessionState) {
        withContext(Dispatchers.IO) {
            require(uri.isNotBlank())
            require(
                (importSourceType == ImportSourceType.Local && domainName == null)
                || (importSourceType == ImportSourceType.Internet && !domainName.isNullOrBlank())
            )
            val currentTime = System.currentTimeMillis()
            queries.add(
                uri = uri,
                source_type = importSourceType.name,
                domain_name = domainName,
                state = sessionState.name,
                creation_datetime = currentTime,
                update_datetime = currentTime
            )
        }
    }

    suspend fun updateState(id: Long, sessionState: ImportSessionState) {
        withContext(Dispatchers.IO) {
            queries.updateState(sessionState.name, System.currentTimeMillis(), id)
        }
    }

    suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) {
            queries.delete(id)
        }
    }
}