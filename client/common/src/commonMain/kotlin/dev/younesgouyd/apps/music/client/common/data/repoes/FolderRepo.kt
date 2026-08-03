package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.Folder
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.rpc.FolderRpc
import io.ktor.client.call.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FolderRepo(
    private val backend: Backend
) {
    fun get(id: FolderId): Flow<Folder?> {
        return flow {
            emit(
                backend.call(FolderRpc.Get(id)).body<Folder?>()
            )
        }
    }

    fun searchFolder(
        folderId: FolderId?,
        nameQuery: String
    ): Flow<List<Folder>> {
        return flow {
            emit(
                backend.call(
                    FolderRpc.SearchFolder(
                        folderId = folderId,
                        nameQuery = nameQuery
                    )
                ).body<List<Folder>>()
            )
        }
    }

    fun getSubfolders(id: FolderId?): Flow<List<Folder>> {
        return flow {
            emit(
                backend.call(FolderRpc.GetSubfolders(id)).body<List<Folder>>()
            )
        }
    }

    suspend fun add(name: String, parentFolderId: FolderId?): FolderId {
        require(name.isNotEmpty())
        return backend.call(
            FolderRpc.Add(
                name = name,
                parentFolderId = parentFolderId
            )
        ).body<FolderId>()
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