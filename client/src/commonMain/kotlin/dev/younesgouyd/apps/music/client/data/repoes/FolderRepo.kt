package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.room.toSearchQuery
import kotlinx.coroutines.flow.Flow

class FolderRepo(private val dao: dev.younesgouyd.apps.music.client.data.room.entities.FolderDao) {
    fun get(id: Long): Flow<dev.younesgouyd.apps.music.client.data.room.entities.Folder> {
        return dao.get(id)
    }

    fun searchFolder(
        folderId: Long?,
        nameQuery: String
    ): Flow<List<dev.younesgouyd.apps.music.client.data.room.entities.Folder>> {
        return dao.searchFolder(folderId, nameQuery.toSearchQuery())
    }

    fun getSubfolders(id: Long?): Flow<List<dev.younesgouyd.apps.music.client.data.room.entities.Folder>> {
        return if (id == null) {
            dao.getRoot()
        } else {
            dao.getSubfolders(id)
        }
    }

    suspend fun add(name: String, parentFolderId: Long?): Long {
        require(name.isNotEmpty())
        val currentTime = System.currentTimeMillis()
        return dao.add(
            name = name,
            parentFolderId = parentFolderId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }

    suspend fun updateName(id: Long, name: String) {
        require(name.isNotEmpty())
        dao.updateName(name, System.currentTimeMillis(), id)
    }

    suspend fun updateParentFolderId(id: Long, parentFolderId: Long?) {
        dao.updateParentFolderId(
            parentFolderId = parentFolderId,
            updateDatetime = System.currentTimeMillis(),
            id = id
        )
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
    }
}