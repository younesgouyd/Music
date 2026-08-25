package dev.younesgouyd.apps.music.client.common.usecases

import dev.younesgouyd.apps.music.client.common.data.Server
import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import dev.younesgouyd.apps.music.common.models.rpc.ClearImportItemRpc

class ClearImportItemUseCase(
    private val server: Server
) {
    suspend fun execute(id: ImportSessionItemId) {
        server.call(ClearImportItemRpc(id))
    }
}