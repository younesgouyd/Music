package dev.younesgouyd.apps.music.client.multiplatform.usecases

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.rpc.DeleteFolderRpc

class DeleteFolderUseCase(
    private val server: Server
) {
    suspend fun execute(id: FolderId) {
        server.call(DeleteFolderRpc(id))
    }
}