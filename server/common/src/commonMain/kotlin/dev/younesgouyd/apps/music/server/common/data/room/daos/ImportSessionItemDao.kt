package dev.younesgouyd.apps.music.server.common.data.room.daos

import androidx.room.Dao
import androidx.room.Query
import dev.younesgouyd.apps.music.common.models.DbOrder
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.server.common.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.server.common.data.room.toSearchQuery
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
    abstract fun getOldest(state: dev.younesgouyd.apps.music.common.models.ImportSessionItem.State): Flow<ImportSessionItem?>

    fun search(
        importSessionId: ImportSessionId,
        state: dev.younesgouyd.apps.music.common.models.ImportSessionItem.State,
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
        state: dev.younesgouyd.apps.music.common.models.ImportSessionItem.State,
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
        state: dev.younesgouyd.apps.music.common.models.ImportSessionItem.State,
        titleQuery: String
    ): Flow<List<ImportSessionItem>>

    @Query("""
        update importsessionitem
        set state = :state,
        audioFileId = :audioFileId,
        updateDatetime = :updateDatetime
        where id = :id
    """)
    abstract suspend fun update(
        state: dev.younesgouyd.apps.music.common.models.ImportSessionItem.State,
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
        state: dev.younesgouyd.apps.music.common.models.ImportSessionItem.State,
        updateDatetime: Long,
        id: ImportSessionItemId
    )
}