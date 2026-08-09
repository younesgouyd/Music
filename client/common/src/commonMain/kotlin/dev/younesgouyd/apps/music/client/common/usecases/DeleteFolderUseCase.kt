package dev.younesgouyd.apps.music.client.common.usecases

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.rpc.DeleteFolderRpc

class DeleteFolderUseCase(
    private val backend: Backend
) {
    suspend fun execute(id: FolderId) {
        backend.call(DeleteFolderRpc(id))
    }
}