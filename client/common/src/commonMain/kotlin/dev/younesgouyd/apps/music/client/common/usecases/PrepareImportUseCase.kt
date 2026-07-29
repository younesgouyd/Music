package dev.younesgouyd.apps.music.client.common.usecases

import dev.younesgouyd.apps.music.common.FolderId
import dev.younesgouyd.apps.music.common.ImportSessionId
import dev.younesgouyd.apps.music.common.Inspection
import io.ktor.client.*

class PrepareImportUseCase(
    private val client: HttpClient
) {
    suspend fun execute(
        selected: List<Long>,
        url: String,
        inspection: Inspection,
        destinationFolderId: FolderId
    ): ImportSessionId {
        TODO()
    }
}