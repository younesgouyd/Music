package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.Inspection
import dev.younesgouyd.apps.music.common.models.rpc.InspectionRpc

class InspectionRepo(
    private val backend: Backend
) {
    suspend fun inspect(url: String): Inspection? {
        return backend.callForResult(InspectionRpc.Inspect(url))
    }
}