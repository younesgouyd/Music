package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.datamodels.Inspection

class InspectionRepo(
    private val server: Server
) {
    suspend fun inspect(url: String): Inspection {
        return server.get(Rpc.ToServer.Inspection.Inspect(url))
    }
}