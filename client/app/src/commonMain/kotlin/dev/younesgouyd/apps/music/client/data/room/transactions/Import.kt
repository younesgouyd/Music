package dev.younesgouyd.apps.music.client.data.room.transactions

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import dev.younesgouyd.apps.music.client.components.util.Uri
import dev.younesgouyd.apps.music.client.data.*
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSession
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem
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
        server: Server,
        fileManager: FileManager,
        getFileName: (Uri) -> String,
        getFileInputStream: (Uri) -> InputStream
    ) {
        withContext(Dispatchers.IO) {
            println("Working on session ${session.id} item ${item.id}")
            val track: Pair<TrackId, MediaFileId>? = when (session.sourceType) {
                ImportSession.SourceType.Local -> {
                    importLocalFile(
                        fileName = getFileName(item.inspection.uri),
                        importSessionItemId = item.id,
                        data = getFileInputStream(item.inspection.uri),
                        folderId = session.destinationFolderId,
                        fileManager = fileManager
                    )
                }
                ImportSession.SourceType.Internet -> {
                    importFromInternet(
                        uri = item.inspection.uri,
                        importSessionItemId = item.id,
                        folderId = session.destinationFolderId,
                        server = server,
                        fileManager = fileManager
                    )
                }
            }
            val state = if (track != null) ImportSessionItem.State.Completed else ImportSessionItem.State.Failed
            updateState(
                state = state,
                audioFileId = track?.second,
                updateDatetime = System.currentTimeMillis(),
                id = item.id
            )
        }
    }

    private suspend fun importLocalFile(
        fileName: String,
        importSessionItemId: ImportSessionItemId,
        data: InputStream,
        folderId: FolderId,
        fileManager: FileManager
    ): Pair<TrackId, MediaFileId> {
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
        val audioMediaFileId = MediaFileId(
            addMediaFile(
                fileName = fileName,
                creationDatetime = System.currentTimeMillis()
            )
        )
        fileManager.saveMediaFile(data, audioMediaFileId)
        return Pair(trackId, audioMediaFileId)
    }

    private suspend fun importFromInternet(
        uri: String,
        importSessionItemId: ImportSessionItemId,
        folderId: FolderId,
        server: Server,
        fileManager: FileManager
    ): Pair<TrackId, MediaFileId>? {
        val result: String = server.download(uri).first()
        return when (result) {
            "error" -> null
            "completed" -> {
                val (filename, stream) = server.getResult()
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
                    val audioMediaFileId = MediaFileId(
                        addMediaFile(
                            fileName = filename,
                            creationDatetime = System.currentTimeMillis()
                        )
                    )
                    fileManager.saveMediaFile(it, audioMediaFileId)
                    Pair(trackId, audioMediaFileId)
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
        folderId: FolderId,
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
