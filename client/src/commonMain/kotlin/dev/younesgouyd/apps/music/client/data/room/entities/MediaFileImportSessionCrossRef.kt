package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.ImportSessionId
import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.MediaFileImportSessionCrossRefId
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity(
    indices = [
        Index(value = ["mediaFileId", "importSessionId"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = MediaFile::class,
            parentColumns = ["id"],
            childColumns = ["mediaFileId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        ),
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
data class MediaFileImportSessionCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: MediaFileImportSessionCrossRefId,
    val mediaFileId: MediaFileId,
    val importSessionId: ImportSessionId,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface MediaFileImportSessionCrossRefDao {
    @Query("select * from mediafileimportsessioncrossref")
    fun getAll(): Flow<List<MediaFileImportSessionCrossRef>>

    @Query("""
        insert into mediafileimportsessioncrossref (mediaFileId, importSessionId, creationDatetime, updateDatetime)
        values (:mediaFileId, :importSessionId, :creationDatetime, :updateDatetime)
    """)
    suspend fun add(
        mediaFileId: MediaFileId,
        importSessionId: ImportSessionId,
        creationDatetime: Long,
        updateDatetime: Long
    )


    @Query("delete from mediafileimportsessioncrossref where mediaFileId = :mediaFileId and importSessionId = :importSessionId")
    suspend fun delete(mediaFileId: MediaFileId, importSessionId: ImportSessionId)
}