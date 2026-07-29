package dev.younesgouyd.apps.music.client.common.usecases

import dev.younesgouyd.apps.music.common.ImportSessionItemId
import io.ktor.client.*

class ClearImportItemUseCase(
    private val client: HttpClient
) {
    suspend fun execute(id: ImportSessionItemId) {
        TODO()
    }
}