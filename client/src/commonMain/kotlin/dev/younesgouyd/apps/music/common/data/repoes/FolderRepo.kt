package dev.younesgouyd.apps.music.common.data.repoes

import dev.younesgouyd.apps.music.common.data.room.entities.Folder
import dev.younesgouyd.apps.music.common.data.room.entities.FolderDao
import kotlinx.coroutines.flow.Flow

class FolderRepo(private val dao: FolderDao) {
    fun getAll(): Flow<List<Folder>> {
        return dao.getAll()
    }

    fun get(id: Long): Flow<Folder> {
        return dao.get(id)
    }

    fun getSubfolders(id: Long?): Flow<List<Folder>> {
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