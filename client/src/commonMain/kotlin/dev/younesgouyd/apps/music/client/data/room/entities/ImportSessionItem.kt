package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.ImportSessionId
import dev.younesgouyd.apps.music.client.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ImportSession::class,
            parentColumns = ["id"],
            childColumns = ["importSessionId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Serializable
data class ImportSessionItem(
    @PrimaryKey(autoGenerate = true)
    val id: ImportSessionItemId,
    val uri: String,
    val importSessionId: ImportSessionId,
    val state: State,
    val inspection: Inspection.ItemInspection,
    val creationDatetime: Long,
    val updateDatetime: Long
) {
    enum class State {
        Nonselected,
        Pending,
        InProgress,
        Completed,
        Cancelled,
        Failed
    }
}

@Dao
interface ImportSessionItemDao {
    @Query("select * from ImportSessionItem")
    fun getAll(): Flow<List<ImportSessionItem>>

    @Query("select * from importsessionitem where id = :id")
    fun get(id: ImportSessionItemId): Flow<ImportSessionItem>

    @Query(
        """
        select * from importsessionitem
        where state = :state
        order by creationDatetime asc
        limit 1
    """
    )
    fun getOldest(state: ImportSessionItem.State): Flow<ImportSessionItem?>

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
    fun search(
        importSessionId: ImportSessionId,
        state: ImportSessionItem.State,
        titleQuery: String
    ): Flow<List<ImportSessionItem>>

    @Query("""
        select i.*
        from importsessionitem i
        join mediafileimportsessionitemcrossref mfisicr on mfisicr.importSessionItemId = i.id
        join mediafiletrackcrossref mftcr on mftcr.mediaFileId = mfisicr.mediaFileId
        where mftcr.trackId = :id
    """)
    fun getTrackImports(id: TrackId): Flow<List<ImportSessionItem>>

    @Query("update importsessionitem set state = :state, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateState(state: ImportSessionItem.State, updateDatetime: Long, id: ImportSessionItemId)
}