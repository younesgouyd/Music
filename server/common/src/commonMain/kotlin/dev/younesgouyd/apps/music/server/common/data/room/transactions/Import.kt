package dev.younesgouyd.apps.music.server.common.data.room.transactions

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import dev.younesgouyd.apps.music.common.*
import dev.younesgouyd.apps.music.server.common.data.FileManager
import dev.younesgouyd.apps.music.server.common.data.YtDlp
import dev.younesgouyd.apps.music.server.common.data.room.entities.ImportSession
import dev.younesgouyd.apps.music.server.common.data.room.entities.ImportSessionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStream

@Dao
abstract class Import {
    @Transaction
    open suspend fun execute(
        session: ImportSession,
        item: ImportSessionItem,
        ytDlp: YtDlp,
        fileManager: FileManager,
        getFileName: (uri: String) -> String,
        getFileInputStream: (uri: String) -> InputStream
    ) {
        withContext(Dispatchers.IO) {
            val result: ImportResult? = import(
                uri = item.inspection.uri,
                importSessionItemId = item.id,
                folderId = session.destinationFolderId,
                ytDlp = ytDlp,
                fileManager = fileManager
            )
            updateState(
                state = if (result != null) ImportSessionItem.State.Completed else ImportSessionItem.State.Failed,
                audioFileId = result?.audioFileId,
                updateDatetime = System.currentTimeMillis(),
                id = item.id
            )
        }
    }

    private data class ImportResult(
        val trackId: TrackId,
        val audioFileId: MediaFileId
    )
    private suspend fun import(
        uri: String,
        importSessionItemId: ImportSessionItemId,
        folderId: FolderId?,
        ytDlp: YtDlp,
        fileManager: FileManager
    ): ImportResult? {
        val result: String = ytDlp.download(uri).first()
        return when (result) {
            "error" -> null
            "completed" -> {
                val (filename, stream) = ytDlp.getResult()
                stream.use {
                    val currentTime = System.currentTimeMillis()
                    val trackId = TrackId(
                        addTrack(
                            importSessionItemId = importSessionItemId,
                            spotifyTrackId = null,
                            folderId = folderId,
                            creationDatetime = currentTime,
                            updateDatetime = currentTime
                        )
                    )
                    val audioMediaFileId =
                        MediaFileId(
                            addMediaFile(
                                fileName = filename,
                                creationDatetime = System.currentTimeMillis()
                            )
                        )
                    fileManager.saveMediaFile(it, audioMediaFileId)
                    ImportResult(trackId, audioMediaFileId)
                }
            }
            else -> TODO()
        }
    }

    @Query("""
        update importsessionitem
        set state = :state,
        audioFileId = :audioFileId,
        updateDatetime = :updateDatetime
        where id = :id
    """)
    protected abstract suspend fun updateState(
        state: ImportSessionItem.State,
        audioFileId: MediaFileId?,
        updateDatetime: Long,
        id: ImportSessionItemId
    )

    @Query(
        """
        insert into track (importSessionItemId, spotifyTrackId, folderId, creationDatetime, updateDatetime)
        values (:importSessionItemId, :spotifyTrackId, :folderId, :creationDatetime, :updateDatetime)
    """
    )
    protected abstract suspend fun addTrack(
        importSessionItemId: ImportSessionItemId,
        spotifyTrackId: SpotifyTrackId?,
        folderId: FolderId?,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long

    @Query(
        """
        insert into mediafile (fileName, creationDatetime)
        values (:fileName, :creationDatetime)
    """
    )
    protected abstract suspend fun addMediaFile(
        fileName: String?,
        creationDatetime: Long
    ): Long
}
