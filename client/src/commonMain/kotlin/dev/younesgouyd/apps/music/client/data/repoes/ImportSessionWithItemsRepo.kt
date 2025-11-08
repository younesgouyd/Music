package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.common.Inspection

class ImportSessionWithItemsRepo(
    private val dao: dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionWithItemsDao,
) {
    suspend fun addLocalSession(inspection: Inspection.Folder) {
        require(inspection.container.uri.isNotBlank() && inspection.items.isNotEmpty())
        dao.addLocalSession(uri = inspection.container.uri, inspection = inspection)
    }

    suspend fun addUrlSession(url: String, inspection: Inspection.Webpage, selected: List<Long>) {
        require(url.isNotBlank() && selected.isNotEmpty())
        dao.addUrlSession(url = url, inspection = inspection, selectedIds = selected)
    }
}
