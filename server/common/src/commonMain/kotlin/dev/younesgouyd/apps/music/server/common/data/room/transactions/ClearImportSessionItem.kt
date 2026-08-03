package dev.younesgouyd.apps.music.server.common.data.room.transactions

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import dev.younesgouyd.apps.music.common.models.ImportSessionItem
import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import dev.younesgouyd.apps.music.common.models.MediaFileId
import io.github.oshai.kotlinlogging.KotlinLogging

@Dao
abstract class ClearImportSessionItem {
    private val logger = KotlinLogging.logger {}

    @Transaction
    open suspend fun execute(id: ImportSessionItemId): MediaFileId {
        val mediaFileId = MediaFileId(getMediaFileId(id))
        deleteTrack(id)
        updateImportSessionItem(id = id)
        deleteMediaFile(mediaFileId)
        logger.info { "::execute | mediaFileId to delete: $mediaFileId" }
        return mediaFileId
    }

    @Query("delete from track where importSessionItemId = :id")
    protected abstract suspend fun deleteTrack(id: ImportSessionItemId)

    @Query("""
        select m.id from mediafile m where m.id = (
            select i.audioFileId from importsessionitem i where i.id = :id
        )
    """)
    protected abstract suspend fun getMediaFileId(id: ImportSessionItemId): Long

    @Query("""
        update importsessionitem
        set state = :state, audioFileId = null, updateDatetime = :updateDatetime
        where id = :id
    """)
    protected abstract suspend fun updateImportSessionItem(
        state: ImportSessionItem.State = ImportSessionItem.State.Nonselected,
        updateDatetime: Long = System.currentTimeMillis(),
        id: ImportSessionItemId
    )

    @Query("delete from mediafile where id = :id")
    protected abstract suspend fun deleteMediaFile(id: MediaFileId)
}