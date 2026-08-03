package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.FolderId
import kotlinx.serialization.Serializable

@Serializable
sealed class FolderRpc : Rpc() {
    @Serializable
    data class Get(
        val id: FolderId
    ) : FolderRpc()

    @Serializable
    data class SearchFolder(
        val folderId: FolderId?,
        val nameQuery: String
    ) : FolderRpc()

    @Serializable
    data class GetSubfolders(
        val id: FolderId?
    ) : FolderRpc()

    @Serializable
    data class Add(
        val name: String,
        val parentFolderId: FolderId?
    ) : FolderRpc()

    @Serializable
    data class UpdateName(
        val id: FolderId,
        val name: String
    ) : FolderRpc()

    @Serializable
    data class UpdateParentFolderId(
        val id: FolderId,
        val parentFolderId: FolderId?
    ) : FolderRpc()

    @Serializable
    data class Delete(
        val id: FolderId
    ) : FolderRpc()
}