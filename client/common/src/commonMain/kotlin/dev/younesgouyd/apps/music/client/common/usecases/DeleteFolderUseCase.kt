package dev.younesgouyd.apps.music.client.common.usecases

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.FolderId

class DeleteFolderUseCase(
    private val backend: Backend
) {
    suspend fun execute(id: FolderId) {
        TODO()
    }
}