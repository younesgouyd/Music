package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.Inspection
import dev.younesgouyd.apps.music.common.models.rpc.InspectionRpc

class InspectionRepo(
    private val server: Server
) {
    suspend fun inspect(url: String): Inspection {
        return server.call<Inspection>(InspectionRpc.Inspect(url))
    }
}