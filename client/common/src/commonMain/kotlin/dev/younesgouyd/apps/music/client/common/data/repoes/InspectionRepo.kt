package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.Inspection
import dev.younesgouyd.apps.music.common.models.rpc.InspectionRpc
import io.ktor.client.call.*

class InspectionRepo(
    private val backend: Backend
) {
    suspend fun inspect(url: String): Inspection? {
        return backend.call(InspectionRpc.Inspect(url)).body<Inspection?>()
    }
}