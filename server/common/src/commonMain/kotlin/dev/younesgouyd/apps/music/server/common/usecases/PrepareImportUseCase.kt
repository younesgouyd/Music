package dev.younesgouyd.apps.music.server.common.usecases

import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.Inspection
import dev.younesgouyd.apps.music.server.common.data.FileManager
import dev.younesgouyd.apps.music.server.common.data.room.transactions.PrepareImport

class PrepareImportUseCase(
    private val fileManager: FileManager,
    private val transaction: PrepareImport
) {
    suspend fun execute(
        selected: List<Long>,
        url: String,
        inspection: Inspection,
        destinationFolderId: FolderId
    ): ImportSessionId {
        return transaction.execute(selected, url, inspection, destinationFolderId, fileManager)
    }
}