package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.common.Inspection
import io.ktor.client.*

class InspectionRepo(
    private val client: HttpClient
) {
    suspend fun inspect(url: String): Inspection? {
        TODO()
    }
}