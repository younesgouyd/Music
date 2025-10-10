package dev.younesgouyd.apps.music.common.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.common.util.ImportSessionState
import dev.younesgouyd.apps.music.common.util.ImportSourceType
import kotlinx.coroutines.flow.Flow

@Entity
data class ImportSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val uri: String,
    val sourceType: ImportSourceType,
    val state: ImportSessionState,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Entity(
    indices = [
        Index(value = ["itemId", "importSessionId"], unique = true),
    ],
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
    val itemId: Long,
    val uri: String,
    val importSessionId: Long,
    val creationDatetime: Long,
    val updateDatetime: Long
)

data class ImportSessionWithItems(
    @Embedded val importSession: ImportSession,
    @Relation(
        parentColumn = "id",
        entityColumn = "importSessionId"
    )
    val items: List<ImportSessionItem>
)

@Dao
interface ImportSessionDao {
    @Transaction
    @Query("select * from importsession")
    fun getAll(): Flow<List<ImportSessionWithItems>>

    @Transaction
    @Query("select * from importsession where id = :id")
    fun get(id: Long): Flow<ImportSessionWithItems>

    @Transaction
    @Query("""
        select *
        from importsession
        where state = 'Pending'
        order by creationDatetime asc
        limit 1
    """)
    fun getOldestPending(): Flow<ImportSessionWithItems?>

    @Query("""
        select count(*) from importsession
        where state in ('Pending', 'Started')
    """)
    fun getOngoingImportsCount(): Flow<Long>

    @Transaction
    suspend fun addUrlSession(uri: String, items: Map<Long, String>) {
        val currentTime = System.currentTimeMillis()
        val sessionId = add(
            uri = uri,
            sourceType = ImportSourceType.Internet,
            state = ImportSessionState.Pending,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
        for ((id, uri) in items) {
            val currentTime = System.currentTimeMillis()
            addImportSessionItem(
                itemId = id,
                uri = uri,
                importSessionId = sessionId,
                creationDatetime = currentTime,
                updateDatetime = currentTime
            )
        }
    }

    @Query("""
        insert into importsession (uri, sourceType, state, creationDatetime, updateDatetime)
        values (:uri, :sourceType, :state, :creationDatetime, :updateDatetime)
    """)
    suspend fun add(uri: String, sourceType: ImportSourceType, state: ImportSessionState, creationDatetime: Long, updateDatetime: Long): Long

    @Query("""
        insert into importsessionitem (itemId, uri, importSessionId, creationDatetime, updateDatetime)
        values (:itemId, :uri, :importSessionId, :creationDatetime, :updateDatetime)
    """)
    suspend fun addImportSessionItem(itemId: Long, uri: String, importSessionId: Long, creationDatetime: Long, updateDatetime: Long): Long

    @Query("update importsession set state = :state, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateState(state: ImportSessionState, updateDatetime: Long, id: Long)

    @Query("delete from importsession where id = :id")
    suspend fun delete(id: Long)
}
