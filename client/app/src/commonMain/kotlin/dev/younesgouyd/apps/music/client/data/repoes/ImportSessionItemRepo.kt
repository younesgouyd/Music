package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.components.util.DbOrder
import dev.younesgouyd.apps.music.client.data.ImportSessionId
import dev.younesgouyd.apps.music.client.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.room.daos.ImportSessionItemDao
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem.State
import dev.younesgouyd.apps.music.client.data.room.toSearchQuery
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.flow.Flow

class ImportSessionItemRepo(
    private val dao: ImportSessionItemDao
) {
    fun get(id: ImportSessionItemId): Flow<ImportSessionItem?> {
        return dao.get(id)
    }

    fun getOldestPending(): Flow<ImportSessionItem?> {
        return dao.getOldest(State.Pending)
    }

    fun search(
        importSessionId: ImportSessionId,
        state: State,
        titleQuery: String,
        order: DbOrder
    ): Flow<List<ImportSessionItem>> {
        return dao.search(importSessionId, state, titleQuery.toSearchQuery(), order)
    }

    suspend fun add(
        uri: String,
        importSessionId: ImportSessionId,
        state: State,
        title: String,
        durationMilliseconds: Long,
        album: String?,
        inspection: Inspection.ItemInspection,
        localFilePath: String?,
        albumTrackNumber: Int?,
        lyrics: String?,
        year: Int?,
        imgId: MediaFileId?
    ): ImportSessionItemId {
        val currentTime = System.currentTimeMillis()
        val id = dao.add(
            uri = uri,
            importSessionId = importSessionId,
            state = state,
            title = title,
            durationMilliseconds = durationMilliseconds,
            album = album,
            inspection = inspection,
            localFilePath = localFilePath,
            albumTrackNumber = albumTrackNumber,
            lyrics = lyrics,
            year = year,
            imgId = imgId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
        return ImportSessionItemId(id)
    }

    suspend fun updateState(
        id: ImportSessionItemId,
        state: State,
        audioFileId: MediaFileId?
    ) {
        dao.update(
            state = state,
            audioFileId = audioFileId,
            updateDatetime = System.currentTimeMillis(),
            id = id
        )
    }

    suspend fun updateState(
        id: ImportSessionItemId,
        state: State
    ) {
        dao.update(
            state = state,
            updateDatetime = System.currentTimeMillis(),
            id = id
        )
    }
}