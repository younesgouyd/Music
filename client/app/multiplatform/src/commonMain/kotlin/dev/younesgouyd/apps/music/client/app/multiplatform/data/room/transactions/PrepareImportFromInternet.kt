package dev.younesgouyd.apps.music.client.app.multiplatform.data.room.transactions

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import dev.younesgouyd.apps.music.client.app.multiplatform.data.FileManager
import dev.younesgouyd.apps.music.client.app.multiplatform.data.FolderId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.ImportSessionId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.MediaFileId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.ImportSession
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64

@Dao
abstract class PrepareImportFromInternet {
    @Transaction
    open suspend fun execute(
        selected: List<Long>,
        url: String,
        inspection:  Inspection.Webpage,
        destinationFolderId: FolderId,
        fileManager: FileManager
    ): ImportSessionId {
        require(url.isNotBlank() && selected.isNotEmpty())
        return withContext(Dispatchers.IO) {
            val sessionId = ImportSessionId(
                addImportSession(
                    uri = url,
                    sourceType = ImportSession.SourceType.Internet,
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
                    localFilePath = null,
                    albumTrackNumber = null,
                    lyrics = null,
                    year = null,
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
        insert into importsession (uri, sourceType, inspection, destinationFolderId, imgId, creationDatetime)
        values (:uri, :sourceType, :inspection, :destinationFolderId, :imgId, :creationDatetime)
    """
    )
    protected abstract suspend fun addImportSession(
        uri: String,
        sourceType: ImportSession.SourceType,
        inspection: Inspection.ContainerInspection,
        destinationFolderId: FolderId,
        imgId: MediaFileId?,
        creationDatetime: Long
    ): Long

    @Query(
        """
        insert into importsessionitem (
            uri, importSessionId, state, title, durationMilliseconds, album, inspection,
            localFilePath, albumTrackNumber, lyrics, year, imgId, creationDatetime, updateDatetime
        ) values (
            :uri, :importSessionId, :state, :title, :durationMilliseconds, :album, :inspection,
            :localFilePath, :albumTrackNumber, :lyrics, :year, :imgId, :creationDatetime, :updateDatetime
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
        inspection: Inspection.ItemInspection,
        localFilePath: String?,
        albumTrackNumber: Int?,
        lyrics: String?,
        year: Int?,
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