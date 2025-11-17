package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.room.entities.Folder
import dev.younesgouyd.apps.music.client.data.room.entities.FolderDao
import dev.younesgouyd.apps.music.client.data.room.toSearchQuery
import kotlinx.coroutines.flow.Flow

class FolderRepo(private val dao: FolderDao) {
    fun get(id: FolderId): Flow<Folder> {
        return dao.get(id)
    }

    fun searchFolder(
        folderId: FolderId?,
        nameQuery: String
    ): Flow<List<Folder>> {
        return dao.searchFolder(folderId, nameQuery.toSearchQuery())
    }

    fun getSubfolders(id: FolderId?): Flow<List<Folder>> {
        return if (id == null) {
            dao.getRoot()
        } else {
            dao.getSubfolders(id)
        }
    }

    suspend fun add(name: String, parentFolderId: FolderId?): FolderId {
        require(name.isNotEmpty())
        val currentTime = System.currentTimeMillis()
        val id = dao.add(
            name = name,
            parentFolderId = parentFolderId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
        return FolderId(id)
    }

    suspend fun updateName(id: FolderId, name: String) {
        require(name.isNotEmpty())
        dao.updateName(name, System.currentTimeMillis(), id)
    }

    suspend fun updateParentFolderId(id: FolderId, parentFolderId: FolderId?) {
        dao.updateParentFolderId(
            parentFolderId = parentFolderId,
            updateDatetime = System.currentTimeMillis(),
            id = id
        )
    }

    suspend fun delete(id: FolderId) {
        dao.delete(id)
    }
}