package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.components.util.DbOrder
import dev.younesgouyd.apps.music.client.data.ImportSessionId
import dev.younesgouyd.apps.music.client.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.room.toSearchQuery
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.flow.Flow

typealias JsonArrayStringOfArtistNames = String

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ImportSession::class,
            parentColumns = ["id"],
            childColumns = ["importSessionId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT // TODO
        ),
        ForeignKey(
            entity = MediaFile::class,
            parentColumns = ["id"],
            childColumns = ["imgId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = MediaFile::class,
            parentColumns = ["id"],
            childColumns = ["audioFileId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["importSessionId"]),
        Index(value = ["imgId"], unique = true),
        Index(value = ["audioFileId"], unique = true)
    ]
)
data class ImportSessionItem(
    @PrimaryKey(autoGenerate = true)
    val id: ImportSessionItemId,
    val uri: String,
    val importSessionId: ImportSessionId,
    val state: State,
    val title: String,
    val durationMilliseconds: Long,
    val album: String?,
    val inspection: Inspection.ItemInspection,
    val localFilePath: String?,
    val albumTrackNumber: Int?,
    val lyrics: String?,
    val year: Int?,
    val imgId: MediaFileId?,
    val audioFileId: MediaFileId?,
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
    fun get(id: ImportSessionItemId): Flow<ImportSessionItem?>

    @Query(
        """
        select * from importsessionitem
        where state = :state
        order by creationDatetime asc
        limit 1
    """
    )
    fun getOldest(state: ImportSessionItem.State): Flow<ImportSessionItem?>

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
    fun searchAsc(
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
    fun searchDesc(
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
    suspend fun add(
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
    suspend fun update(
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
    suspend fun update(
        state: ImportSessionItem.State,
        updateDatetime: Long,
        id: ImportSessionItemId
    )
}