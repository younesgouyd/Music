package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.FileManager
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionWithItemsDao
import dev.younesgouyd.apps.music.common.Inspection

class ImportSessionWithItemsRepo(
    private val dao: ImportSessionWithItemsDao,
    private val fileManager: FileManager
) {
    suspend fun addLocalSession(inspection: Inspection.Folder, destinationFolderId: Long?) {
        require(inspection.container.uri.isNotBlank() && inspection.items.isNotEmpty())
        dao.addLocalSession(
            uri = inspection.container.uri,
            inspection = inspection,
            destinationFolderId = destinationFolderId
        )
    }

    suspend fun addUrlSession(
        url: String,
        inspection: Inspection.Webpage,
        ytDlpInspection: String,
        selected: List<Long>,
        destinationFolderId: Long?
    ) {
        require(url.isNotBlank() && selected.isNotEmpty())
        val sessionId = dao.addUrlSession(
            url = url,
            inspection = inspection,
            selectedIds = selected,
            destinationFolderId = destinationFolderId
        )
        fileManager.saveYtDlpInspection(sessionId, ytDlpInspection)
    }
}
