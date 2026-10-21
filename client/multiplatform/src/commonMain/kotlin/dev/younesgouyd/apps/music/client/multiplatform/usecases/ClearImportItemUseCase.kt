package dev.younesgouyd.apps.music.client.multiplatform.usecases

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import dev.younesgouyd.apps.music.common.models.Rpc

class ClearImportItemUseCase(
    private val server: Server
) {
    suspend fun execute(id: ImportSessionItemId) {
        server.call(Rpc.ToServer.ClearImportItem(id))
    }
}