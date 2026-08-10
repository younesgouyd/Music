package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.Folder
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.rpc.FolderRpc
import kotlinx.coroutines.flow.Flow

class FolderRepo(
    private val backend: Backend
) {
    fun get(id: FolderId): Flow<Folder?> {
        return backend.stream(FolderRpc.Get(id))
    }

    fun searchFolder(
        folderId: FolderId?,
        nameQuery: String
    ): Flow<List<Folder>> {
        return backend.stream(
            FolderRpc.SearchFolder(
                folderId = folderId,
                nameQuery = nameQuery
            )
        )
    }

    fun getSubfolders(id: FolderId?): Flow<List<Folder>> {
        return backend.stream(FolderRpc.GetSubfolders(id))
    }

    suspend fun add(name: String, parentFolderId: FolderId?): FolderId {
        require(name.isNotEmpty())
        return backend.callForResult(
            FolderRpc.Add(
                name = name,
                parentFolderId = parentFolderId
            )
        )
    }

    suspend fun updateName(id: FolderId, name: String) {
        require(name.isNotEmpty())
        backend.call(
            FolderRpc.UpdateName(
                id = id,
                name = name
            )
        )
    }

    suspend fun updateParentFolderId(id: FolderId, parentFolderId: FolderId?) {
        backend.call(
            FolderRpc.UpdateParentFolderId(
                id = id,
                parentFolderId = parentFolderId
            )
        )
    }

    suspend fun delete(id: FolderId) {
        backend.call(FolderRpc.Delete(id))
    }
}