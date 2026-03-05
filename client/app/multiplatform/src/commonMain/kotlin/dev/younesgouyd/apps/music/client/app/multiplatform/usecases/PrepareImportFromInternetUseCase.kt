package dev.younesgouyd.apps.music.client.app.multiplatform.usecases

import dev.younesgouyd.apps.music.client.app.multiplatform.data.FileManager
import dev.younesgouyd.apps.music.client.app.multiplatform.data.FolderId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.ImportSessionId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.transactions.PrepareImportFromInternet
import dev.younesgouyd.apps.music.common.Inspection

class PrepareImportFromInternetUseCase(
    private val fileManager: FileManager,
    private val transaction: PrepareImportFromInternet
) {
    suspend fun execute(
        selected: List<Long>,
        url: String,
        inspection:  Inspection.Webpage,
        destinationFolderId: FolderId
    ): ImportSessionId {
        return transaction.execute(selected, url, inspection, destinationFolderId, fileManager)
    }
}