package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.datamodels.Folder
import kotlinx.coroutines.flow.Flow

class FolderRepo(
    private val server: Server
) {
    fun get(id: FolderId): Flow<Folder?> {
        return server.observe(Rpc.ToServer.Folder.Get(id))
    }

    fun searchFolder(
        folderId: FolderId?,
        nameQuery: String
    ): Flow<List<Folder>> {
        return server.observe(
            Rpc.ToServer.Folder.SearchFolder(
                folderId = folderId,
                nameQuery = nameQuery
            )
        )
    }

    suspend fun add(name: String, parentFolderId: FolderId?): FolderId {
        require(name.isNotEmpty())
        return server.get(
            Rpc.ToServer.Folder.Add(
                name = name,
                parentFolderId = parentFolderId
            )
        )
    }

    suspend fun updateName(id: FolderId, name: String) {
        require(name.isNotEmpty())
        server.call(
            Rpc.ToServer.Folder.UpdateName(
                id = id,
                name = name
            )
        )
    }
}