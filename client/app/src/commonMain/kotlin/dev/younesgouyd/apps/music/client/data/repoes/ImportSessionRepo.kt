package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.ImportSessionId
import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.room.daos.ImportSessionDao
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSession
import dev.younesgouyd.apps.music.client.util.Offset
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.flow.Flow

class ImportSessionRepo(
    private val dao: ImportSessionDao
) {
    suspend fun getAll(limit: Int, offset: Offset.Index): List<ImportSession> {
        return dao.getAll(limit, offset.value)
    }

    fun get(id: ImportSessionId): Flow<ImportSession?> {
        return dao.get(id)
    }

    suspend fun add(
        uri: String,
        sourceType: ImportSession.SourceType,
        inspection: Inspection.ContainerInspection,
        destinationFolderId: FolderId,
        imgId: MediaFileId?
    ): ImportSessionId {
        val id = dao.add(
            uri = uri,
            sourceType = sourceType,
            inspection = inspection,
            destinationFolderId = destinationFolderId,
            imgId = imgId,
            creationDatetime = System.currentTimeMillis()
        )
        return ImportSessionId(id)
    }
}