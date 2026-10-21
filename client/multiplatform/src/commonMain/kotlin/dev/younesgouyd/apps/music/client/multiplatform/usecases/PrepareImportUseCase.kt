package dev.younesgouyd.apps.music.client.multiplatform.usecases

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.datamodels.Inspection

class PrepareImportUseCase(
    private val server: Server
) {
    suspend fun execute(
        selected: List<Long>,
        url: String,
        inspection: Inspection,
        destinationFolderId: FolderId
    ): ImportSessionId {
        return server.get(
            Rpc.ToServer.PrepareImport(
                selected = selected,
                url = url,
                inspection = inspection,
                destinationFolderId = destinationFolderId
            )
        )
    }
}