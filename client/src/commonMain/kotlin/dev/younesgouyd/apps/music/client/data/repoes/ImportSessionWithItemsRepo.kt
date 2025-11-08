package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.common.Inspection

class ImportSessionWithItemsRepo(
    private val dao: dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionWithItemsDao,
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
        selected: List<Long>,
        destinationFolderId: Long?
    ) {
        require(url.isNotBlank() && selected.isNotEmpty())
        dao.addUrlSession(
            url = url,
            inspection = inspection,
            selectedIds = selected,
            destinationFolderId = destinationFolderId
        )
    }
}
