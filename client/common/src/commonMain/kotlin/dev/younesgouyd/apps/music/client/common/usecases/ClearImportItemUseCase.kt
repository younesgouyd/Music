package dev.younesgouyd.apps.music.client.common.usecases

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import dev.younesgouyd.apps.music.common.models.rpc.ClearImportItemRpc

class ClearImportItemUseCase(
    private val backend: Backend
) {
    suspend fun execute(id: ImportSessionItemId) {
        backend.call(ClearImportItemRpc(id))
    }
}