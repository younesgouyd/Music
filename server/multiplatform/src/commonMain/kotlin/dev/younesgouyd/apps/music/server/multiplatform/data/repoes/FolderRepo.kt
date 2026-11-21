package dev.younesgouyd.apps.music.server.multiplatform.data.repoes

import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.server.multiplatform.data.room.daos.FolderDao
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.Folder
import dev.younesgouyd.apps.music.server.multiplatform.data.room.toSearchQuery
import kotlinx.coroutines.flow.Flow

class FolderRepo(
    private val dao: FolderDao
) {
    fun get(id: FolderId): Flow<Folder?> {
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

    fun getPath(target: FolderId): Flow<List<Folder>> {
        return dao.getPath(target)
    }

    suspend fun add(name: String, parentFolderId: FolderId?): FolderId {
        require(name.isNotBlank())
        val currentTime = System.currentTimeMillis()
        val id = dao.add(
            name = name.trim(),
            parentFolderId = parentFolderId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
        return FolderId(id)
    }

    suspend fun updateName(id: FolderId, name: String) {
        require(name.isNotBlank())
        dao.updateName(name.trim(), System.currentTimeMillis(), id)
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