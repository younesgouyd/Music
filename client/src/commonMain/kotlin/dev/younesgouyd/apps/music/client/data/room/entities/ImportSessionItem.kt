package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.flow.Flow

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
data class ImportSessionItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val uri: String,
    val importSessionId: Long,
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
    @Query("select * from importsessionitem where id = :id")
    fun get(id: Long): Flow<ImportSessionItem>

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
        importSessionId: Long,
        state: ImportSessionItem.State,
        titleQuery: String
    ): Flow<List<ImportSessionItem>>

    @Query("update importsessionitem set state = :state, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateState(state: ImportSessionItem.State, updateDatetime: Long, id: Long)
}