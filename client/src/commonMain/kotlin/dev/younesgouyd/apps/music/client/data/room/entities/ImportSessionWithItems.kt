package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import dev.younesgouyd.apps.music.common.Inspection
import dev.younesgouyd.apps.music.common.Inspection.ItemInspection

@Dao
interface ImportSessionWithItemsDao {
    @Transaction
    suspend fun addUrlSession(
        url: String,
        inspection: Inspection.Webpage,
        selectedIds: List<Long>,
        destinationFolderId: Long?
    ): Pair<Long, Map<Long, ItemInspection.InternetTrack>> {
        if (selectedIds.isEmpty()) TODO()
        val currentTime = System.currentTimeMillis()
        val sessionId = add(
            uri = url,
            sourceType = ImportSession.SourceType.Internet,
            inspection = inspection.container.copy(thumbnail = null),
            destinationFolderId = destinationFolderId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
        val itemsWithId = mutableMapOf<Long, ItemInspection.InternetTrack>()
        for (item in inspection.items) {
            val currentTime = System.currentTimeMillis()
            val itemId = addImportSessionItem(
                uri = item.uri,
                importSessionId = sessionId,
                state = if (selectedIds.contains(item.id)) ImportSessionItem.State.Pending else ImportSessionItem.State.Nonselected,
                inspection = item.copy(thumbnail = null),
                creationDatetime = currentTime,
                updateDatetime = currentTime
            )
            itemsWithId[itemId] = item
        }
        return sessionId to itemsWithId
    }

    @Transaction
    suspend fun addLocalSession(
        uri: String,
        inspection: Inspection.Folder,
        destinationFolderId: Long?
    ): Pair<Long, Map<Long, ItemInspection.LocalFileTrack>> {
        val currentTime = System.currentTimeMillis()
        val sessionId = add(
            uri = uri,
            sourceType = ImportSession.SourceType.Local,
            inspection = inspection.container,
            destinationFolderId = destinationFolderId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
        val itemsWithId = mutableMapOf<Long, ItemInspection.LocalFileTrack>()
        for (item in inspection.items) {
            val currentTime = System.currentTimeMillis()
            val itemId = addImportSessionItem(
                uri = item.uri,
                importSessionId = sessionId,
                state = ImportSessionItem.State.Pending,
                inspection = item.copy(albumImage = null),
                creationDatetime = currentTime,
                updateDatetime = currentTime
            )
            itemsWithId[itemId] = item
        }
        return sessionId to itemsWithId
    }

    @Query(
        """
        insert into importsession (uri, sourceType, inspection, destinationFolderId, creationDatetime, updateDatetime)
        values (:uri, :sourceType, :inspection, :destinationFolderId, :creationDatetime, :updateDatetime)
    """
    )
    suspend fun add(
        uri: String,
        sourceType: ImportSession.SourceType,
        inspection: Inspection.ContainerInspection,
        destinationFolderId: Long?,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long

    @Query(
        """
        insert into importsessionitem (uri, importSessionId, state, inspection, creationDatetime, updateDatetime)
        values (:uri, :importSessionId, :state, :inspection, :creationDatetime, :updateDatetime)
    """
    )
    suspend fun addImportSessionItem(
        uri: String,
        importSessionId: Long,
        state: ImportSessionItem.State,
        inspection: Inspection.ItemInspection,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long
}
