package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.Folder
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.rpc.FolderRpc
import kotlinx.coroutines.flow.Flow

class FolderRepo(
    private val server: Server
) {
    fun get(id: FolderId): Flow<Folder?> {
        return server.observe(FolderRpc.Get(id))
    }

    fun searchFolder(
        folderId: FolderId?,
        nameQuery: String
    ): Flow<List<Folder>> {
        return server.observe(
            FolderRpc.SearchFolder(
                folderId = folderId,
                nameQuery = nameQuery
            )
        )
    }

    fun getSubfolders(id: FolderId?): Flow<List<Folder>> {
        return server.observe(FolderRpc.GetSubfolders(id))
    }

    suspend fun add(name: String, parentFolderId: FolderId?): FolderId {
        require(name.isNotEmpty())
        return server.call(
            FolderRpc.Add(
                name = name,
                parentFolderId = parentFolderId
            )
        )
    }

    suspend fun updateName(id: FolderId, name: String) {
        require(name.isNotEmpty())
        server.call<Unit>(
            FolderRpc.UpdateName(
                id = id,
                name = name
            )
        )
    }

    suspend fun updateParentFolderId(id: FolderId, parentFolderId: FolderId?) {
        server.call<Unit>(
            FolderRpc.UpdateParentFolderId(
                id = id,
                parentFolderId = parentFolderId
            )
        )
    }

    suspend fun delete(id: FolderId) {
        server.call<Unit>(FolderRpc.Delete(id))
    }
}