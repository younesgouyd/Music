package dev.younesgouyd.apps.music.client.app.multiplatform.data.room.daos

import androidx.room.Dao
import androidx.room.Query
import dev.younesgouyd.apps.music.client.app.multiplatform.components.util.DbOrder
import dev.younesgouyd.apps.music.client.app.multiplatform.data.ImportSessionId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.MediaFileId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.toSearchQuery
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ImportSessionItemDao {
    @Query("select * from importsessionitem where id = :id")
    abstract fun get(id: ImportSessionItemId): Flow<ImportSessionItem?>

    @Query(
        """
        select * from importsessionitem
        where state = :state
        order by creationDatetime asc
        limit 1
    """
    )
    abstract fun getOldest(state: ImportSessionItem.State): Flow<ImportSessionItem?>

    fun search(
        importSessionId: ImportSessionId,
        state: ImportSessionItem.State,
        titleQuery: String,
        order: DbOrder
    ): Flow<List<ImportSessionItem>> {
        return when (order) {
            DbOrder.Ascending -> searchAsc(importSessionId, state, titleQuery.toSearchQuery())
            DbOrder.Descending -> searchDesc(importSessionId, state, titleQuery.toSearchQuery())
        }
    }

    @Query(
        """
        select *
        from importsessionitem
        where importSessionId = :importSessionId
        and state = :state
        and inspection like '%"title":"' || :titleQuery || '"%' -- TODO: this is not working
        order by creationDatetime asc
    """
    )
    abstract fun searchAsc(
        importSessionId: ImportSessionId,
        state: ImportSessionItem.State,
        titleQuery: String
    ): Flow<List<ImportSessionItem>>

    @Query(
        """
        select *
        from importsessionitem
        where importSessionId = :importSessionId
        and state = :state
        and inspection like '%"title":"' || :titleQuery || '"%' -- TODO: this is not working
        order by creationDatetime desc
    """
    )
    abstract fun searchDesc(
        importSessionId: ImportSessionId,
        state: ImportSessionItem.State,
        titleQuery: String
    ): Flow<List<ImportSessionItem>>

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
    abstract suspend fun add(
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

    @Query("""
        update importsessionitem
        set state = :state,
        audioFileId = :audioFileId,
        updateDatetime = :updateDatetime
        where id = :id
    """)
    abstract suspend fun update(
        state: ImportSessionItem.State,
        audioFileId: MediaFileId?,
        updateDatetime: Long,
        id: ImportSessionItemId
    )

    @Query("""
        update importsessionitem
        set state = :state,
        updateDatetime = :updateDatetime
        where id = :id
    """)
    abstract suspend fun update(
        state: ImportSessionItem.State,
        updateDatetime: Long,
        id: ImportSessionItemId
    )
}