package dev.younesgouyd.apps.music.server.common.data.room.transactions

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import dev.younesgouyd.apps.music.common.FolderId
import dev.younesgouyd.apps.music.common.ImportSessionId
import dev.younesgouyd.apps.music.common.Inspection
import dev.younesgouyd.apps.music.common.MediaFileId
import dev.younesgouyd.apps.music.server.common.data.FileManager
import dev.younesgouyd.apps.music.server.common.data.room.entities.ImportSessionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64

@Dao
abstract class PrepareImport {
    @Transaction
    open suspend fun execute(
        selected: List<Long>,
        url: String,
        inspection:  Inspection,
        destinationFolderId: FolderId,
        fileManager: FileManager
    ): ImportSessionId {
        require(url.isNotBlank() && selected.isNotEmpty())
        return withContext(Dispatchers.IO) {
            val sessionId = ImportSessionId(
                addImportSession(
                    uri = url,
                    inspection = inspection.container.copy(thumbnail = null),
                    destinationFolderId = destinationFolderId,
                    imgId = inspection.container.thumbnail?.let { thumbnail ->
                        val data = Base64.decode(thumbnail)
                        val id = MediaFileId(
                            addMediaFile(fileName = null, creationDatetime = System.currentTimeMillis())
                        )
                        fileManager.saveMediaFile(data, id)
                        id
                    },
                    creationDatetime = System.currentTimeMillis()
                )
            )
            for (item in inspection.items) {
                val currentTime = System.currentTimeMillis()
                addImportSessionItem(
                    uri = item.uri,
                    importSessionId = sessionId,
                    state = if (selected.contains(item.id)) ImportSessionItem.State.Pending else ImportSessionItem.State.Nonselected,
                    title = item.title,
                    durationMilliseconds = item.durationMilliseconds,
                    album = item.album,
                    inspection = item.copy(thumbnail = null),
                    imgId = item.thumbnail?.let { thumbnail ->
                        val data = Base64.decode(thumbnail)
                        val id = MediaFileId(
                            addMediaFile(fileName = null, creationDatetime = System.currentTimeMillis())
                        )
                        fileManager.saveMediaFile(data, id)
                        id
                    },
                    creationDatetime = currentTime,
                    updateDatetime = currentTime
                )
            }
            return@withContext sessionId
        }
    }

    @Query(
        """
        insert into importsession (uri, inspection, destinationFolderId, imgId, creationDatetime)
        values (:uri, :inspection, :destinationFolderId, :imgId, :creationDatetime)
    """
    )
    protected abstract suspend fun addImportSession(
        uri: String,
        inspection: Inspection.Container,
        destinationFolderId: FolderId,
        imgId: MediaFileId?,
        creationDatetime: Long
    ): Long

    @Query(
        """
        insert into importsessionitem (
            uri, importSessionId, state, title, durationMilliseconds, album, inspection,
            imgId, creationDatetime, updateDatetime
        ) values (
            :uri, :importSessionId, :state, :title, :durationMilliseconds, :album, :inspection,
            :imgId, :creationDatetime, :updateDatetime
        )
    """
    )
    abstract suspend fun addImportSessionItem(
        uri: String,
        importSessionId: ImportSessionId,
        state: ImportSessionItem.State,
        title: String,
        durationMilliseconds: Long,
        album: String?,
        inspection: Inspection.Item,
        imgId: MediaFileId?,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long

    @Query(
        """
        insert into mediafile (fileName, creationDatetime)
        values (:fileName, :creationDatetime)
    """
    )
    abstract suspend fun addMediaFile(
        fileName: String?,
        creationDatetime: Long
    ): Long
}