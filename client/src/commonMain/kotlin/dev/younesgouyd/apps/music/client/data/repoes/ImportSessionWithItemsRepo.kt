package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.FileManager
import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.ImportSessionId
import dev.younesgouyd.apps.music.client.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionWithItemsDao
import dev.younesgouyd.apps.music.common.Inspection
import dev.younesgouyd.apps.music.common.Inspection.ItemInspection

class ImportSessionWithItemsRepo(
    private val dao: ImportSessionWithItemsDao,
    private val fileManager: FileManager
) {
    suspend fun addLocalSession(
        inspection: Inspection.Folder,
        destinationFolderId: FolderId?
    ): Pair<ImportSessionId, Map<ImportSessionItemId, ItemInspection.LocalFileTrack>> {
        require(inspection.container.uri.isNotBlank() && inspection.items.isNotEmpty())
        return dao.addLocalSession(
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
        destinationFolderId: FolderId?
    ): Pair<ImportSessionId, Map<ImportSessionItemId, ItemInspection.InternetTrack>> {
        require(url.isNotBlank() && selected.isNotEmpty())
        val ids: Pair<ImportSessionId, Map<ImportSessionItemId, ItemInspection.InternetTrack>> = dao.addUrlSession(
            url = url,
            inspection = inspection,
            selectedIds = selected,
            destinationFolderId = destinationFolderId
        )
        fileManager.saveYtDlpInspection(ids.first, ytDlpInspection)
        return ids
    }
}
