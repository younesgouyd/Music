package dev.younesgouyd.apps.music.app.data.repoes

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import dev.younesgouyd.apps.music.app.data.sqldelight.migrations.Folder
import dev.younesgouyd.apps.music.app.data.sqldelight.queries.FolderQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant

class FolderRepo(private val queries: FolderQueries) {
    fun getAll(): Flow<List<Folder>> {
        return queries.getAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun get(id: Long): Flow<Folder> {
        return queries.get(id)
            .asFlow()
            .mapToOne(Dispatchers.IO)
    }

    suspend fun getStatic(id: Long): Folder? {
        return withContext(Dispatchers.IO) {
            queries.get(id).executeAsOneOrNull()
        }
    }

    fun getSubfolders(id: Long?): Flow<List<Folder>> {
        return queries.getSubfolders(id)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    suspend fun getSubfoldersStatic(id: Long?): List<Folder> {
        return withContext(Dispatchers.IO) {
            queries.getSubfolders(id).executeAsList()
        }
    }

    suspend fun add(name: String, parentFolderId: Long?): Long {
        require(name.isNotEmpty())
        return withContext(Dispatchers.IO) {
            val currentTime = Instant.now().toEpochMilli()
            queries.add(
                name = name,
                parentFolderId = parentFolderId,
                creation_datetime = currentTime,
                update_datetime = currentTime
            ).executeAsOne()
        }
    }

    suspend fun updateName(id: Long, name: String) {
        require(name.isNotEmpty())
        withContext(Dispatchers.IO) {
            queries.updateName(name, Instant.now().toEpochMilli(), id)
        }
    }

    suspend fun updateParentFolderId(id: Long, parentFolderId: Long?) {
        withContext(Dispatchers.IO) {
            queries.updateParentFolderId(
                parent_folder_id = parentFolderId,
                update_datetime = Instant.now().toEpochMilli(),
                id = id
            )
        }
    }

    suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) {
            queries.delete(id)
        }
    }
}