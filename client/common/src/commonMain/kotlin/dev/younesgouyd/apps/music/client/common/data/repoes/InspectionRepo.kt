package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Server
import dev.younesgouyd.apps.music.common.models.Inspection
import dev.younesgouyd.apps.music.common.models.rpc.InspectionRpc

class InspectionRepo(
    private val server: Server
) {
    suspend fun inspect(url: String): Inspection? {
        return server.callForResult(InspectionRpc.Inspect(url))
    }
}