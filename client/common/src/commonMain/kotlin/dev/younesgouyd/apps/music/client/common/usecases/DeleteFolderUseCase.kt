package dev.younesgouyd.apps.music.client.common.usecases

import dev.younesgouyd.apps.music.common.FolderId
import io.ktor.client.*

class DeleteFolderUseCase(
    private val client: HttpClient
) {
    suspend fun execute(id: FolderId) {
        TODO()
    }
}