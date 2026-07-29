package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.common.Folder
import dev.younesgouyd.apps.music.common.FolderId
import io.ktor.client.*
import kotlinx.coroutines.flow.Flow

class FolderRepo(
    private val client: HttpClient
) {
    fun get(id: FolderId): Flow<Folder?> {
        TODO()
    }

    fun searchFolder(
        folderId: FolderId?,
        nameQuery: String
    ): Flow<List<Folder>> {
        TODO()
    }

    fun getSubfolders(id: FolderId?): Flow<List<Folder>> {
        TODO()
//        return if (id == null) {
//            dao.getRoot()
//        } else {
//            dao.getSubfolders(id)
//        }
    }

    suspend fun add(name: String, parentFolderId: FolderId?): FolderId {
        TODO()
//        require(name.isNotEmpty())
//        val currentTime = System.currentTimeMillis()
//        val id = dao.add(
//            name = name,
//            parentFolderId = parentFolderId,
//            creationDatetime = currentTime,
//            updateDatetime = currentTime
//        )
//        return FolderId(id)
    }

    suspend fun updateName(id: FolderId, name: String) {
        TODO()
//        require(name.isNotEmpty())
//        dao.updateName(name, System.currentTimeMillis(), id)
    }

    suspend fun updateParentFolderId(id: FolderId, parentFolderId: FolderId?) {
        TODO()
//        dao.updateParentFolderId(
//            parentFolderId = parentFolderId,
//            updateDatetime = System.currentTimeMillis(),
//            id = id
//        )
    }

    suspend fun delete(id: FolderId) {
        TODO()
//        dao.delete(id)
    }
}