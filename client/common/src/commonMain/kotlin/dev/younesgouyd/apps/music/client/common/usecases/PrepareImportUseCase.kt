package dev.younesgouyd.apps.music.client.common.usecases

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.Inspection
import dev.younesgouyd.apps.music.common.models.rpc.PrepareImportRpc

class PrepareImportUseCase(
    private val backend: Backend
) {
    suspend fun execute(
        selected: List<Long>,
        url: String,
        inspection: Inspection,
        destinationFolderId: FolderId
    ): ImportSessionId {
        return backend.callForResult<ImportSessionId>(
            PrepareImportRpc(
                selected = selected,
                url = url,
                inspection = inspection,
                destinationFolderId = destinationFolderId
            )
        )
    }
}