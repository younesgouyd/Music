package dev.younesgouyd.apps.music.common.data.repoes

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.younesgouyd.apps.music.common.data.sqldelight.migrations.Import_session
import dev.younesgouyd.apps.music.common.data.sqldelight.queries.ImportSessionItemQueries
import dev.younesgouyd.apps.music.common.data.sqldelight.queries.ImportSessionQueries
import dev.younesgouyd.apps.music.common.util.ImportSessionState
import dev.younesgouyd.apps.music.common.util.ImportSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ImportSessionRepo(
    private val queries: ImportSessionQueries,
    private val itemQueries: ImportSessionItemQueries
) {
    fun getAll(): Flow<List<ImportSession>> {
        return queries.getAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map { it.toModel() }
            }
    }

    fun get(id: Long): Flow<ImportSession> {
        return queries.get(id)
            .asFlow()
            .mapToOne(Dispatchers.IO)
            .map { it.toModel() }
    }

    fun getOldestPending(): Flow<ImportSession?> {
        return queries.getOldestPending()
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toModel() }
    }

    fun getOngoingImportsCount(): Flow<Long> {
        return queries.getOngoingImportsCount()
            .asFlow()
            .mapToOne(Dispatchers.IO)
    }

    suspend fun addLocalSession(uri: String) {
        println("--> ::addLocalSession")
        require(uri.isNotBlank())
        withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            queries.add(
                uri = uri,
                source_type = ImportSourceType.Local.name,
                state = ImportSessionState.Pending.name,
                creation_datetime = currentTime,
                update_datetime = currentTime
            ).executeAsOne()
        }
    }

    suspend fun addUrlSession(uri: String, items: Map<Long, String>) {
        require(uri.isNotBlank() && items.isNotEmpty())
        return withContext(Dispatchers.IO) {
            queries.transaction {
                val currentTime = System.currentTimeMillis()
                val sessionId = queries.add(
                    uri = uri,
                    source_type = ImportSourceType.Internet.name,
                    state = ImportSessionState.Pending.name,
                    creation_datetime = currentTime,
                    update_datetime = currentTime
                ).executeAsOne()
                for ((id, url) in items) {
                    itemQueries.add(
                        item_id = id,
                        url = url,
                        import_session_id = sessionId,
                        creation_datetime = System.currentTimeMillis()
                    )
                }
            }
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

    private fun Import_session.toModel(): ImportSession {
        return ImportSession(
            id = this.id,
            uri = this.uri,
            sourceType = ImportSourceType.valueOf(this.source_type),
            state = ImportSessionState.valueOf(this.state),
            creationDatetime = this.creation_datetime,
            items = itemQueries.getImportSessionItems(import_session_id = this.id)
                .executeAsList()
                .map {
                    ImportSession.Item(
                        id = it.id,
                        url = it.url
                    )
                }
        )
    }

    data class ImportSession(
        val id: Long,
        val uri: String,
        val sourceType: ImportSourceType,
        val state: ImportSessionState,
        val creationDatetime: Long,
        val items: List<Item>
    ) {
        data class Item(
            val id: Long,
            val url: String
        )
    }
}
