package dev.younesgouyd.apps.music.server.multiplatform.data.room.transactions

import androidx.room3.Dao
import androidx.room3.Query
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.datamodels.ImportSessionItem.State
import dev.younesgouyd.apps.music.server.multiplatform.YtDlp
import dev.younesgouyd.apps.music.server.multiplatform.data.FileManager
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.ImportSession
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.ImportSessionItem
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Dao
abstract class Import {
    private val logger = KotlinLogging.logger {}

//    @Transaction // TODO
    open suspend fun execute(
        session: ImportSession,
        item: ImportSessionItem,
        ytDlp: YtDlp,
        fileManager: FileManager
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
                state = if (result != null) State.Completed else State.Failed,
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
                val trackId = tryAddTrack(importSessionItemId, folderId) ?: return null
                try {
                    var audioMediaFileId: MediaFileId? = null
                    ytDlp.getResult { filename, channel ->
                        audioMediaFileId = tryAddMediaFileId(filename, trackId) ?: return@getResult null
                        tryWriteFile(fileManager, channel, audioMediaFileId, trackId) ?: return@getResult null
                        ImportResult(trackId, audioMediaFileId)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "could not get download result. rollbacking..." }
                    tryDeleteTrack(trackId)
                    null
                }
            }
            else -> null
        }
    }

    suspend fun tryAddTrack(
        importSessionItemId: ImportSessionItemId,
        folderId: FolderId?
    ): TrackId? {
        return try {
            val currentTime = System.currentTimeMillis()
            TrackId(
                addTrack(
                    importSessionItemId = importSessionItemId,
                    spotifyTrackId = null,
                    folderId = folderId,
                    creationDatetime = currentTime,
                    updateDatetime = currentTime
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "could not add track" }
            null
        }
    }

    suspend fun tryAddMediaFileId(fileName: String, trackId: TrackId): MediaFileId? {
        return try {
            MediaFileId(
                addMediaFile(
                    fileName = fileName,
                    creationDatetime = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "could not add MediaFile. rollbacking..." }
            tryDeleteTrack(trackId)
            null
        }
    }

    private suspend fun tryWriteFile(
        fileManager: FileManager,
        channel: ByteReadChannel,
        audioMediaFileId: MediaFileId,
        trackId: TrackId
    ): Unit? {
        return try {
            fileManager.saveMediaFile(channel, audioMediaFileId)
        } catch (e: Exception) {
            logger.error(e) { "could not write MediaFile file relating to MediaFile $audioMediaFileId. rollbacking..." }
            fileManager.rollback(audioMediaFileId)
            tryDeleteMediaFileId(audioMediaFileId)
            tryDeleteTrack(trackId)
            null
        }
    }

    suspend fun tryDeleteTrack(id: TrackId) {
        withContext(NonCancellable) {
            try {
                deleteTrack(id)
            } catch (e: Exception) {
                logger.error(e) { "could not delete track $id" }
            }
        }
    }

    suspend fun tryDeleteMediaFileId(id: MediaFileId) {
        withContext(NonCancellable) {
            try {
                deleteMediaFile(id)
            } catch (e: Exception) {
                logger.error(e) { "could not delete MediaFile $id" }
            }
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
        state: State,
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

    @Query("delete from track where id = :id")
    protected abstract suspend fun deleteTrack(id: TrackId)

    @Query("delete from mediafile where id = :id")
    protected abstract suspend fun deleteMediaFile(id: MediaFileId)
}
