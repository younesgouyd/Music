package dev.younesgouyd.apps.music.client.multiplatform.usecases

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.Rpc

class DeleteFolderUseCase(
    private val server: Server
) {
    suspend fun execute(id: FolderId) {
        server.call(Rpc.ToServer.DeleteFolder(id))
    }
}