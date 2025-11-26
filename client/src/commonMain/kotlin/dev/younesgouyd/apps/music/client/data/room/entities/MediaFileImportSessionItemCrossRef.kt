package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.MediaFileImportSessionItemCrossRefId
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity(
    indices = [
        Index(value = ["mediaFileId", "importSessionItemId"], unique = true),
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
            entity = ImportSessionItem::class,
            parentColumns = ["id"],
            childColumns = ["importSessionItemId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Serializable
data class MediaFileImportSessionItemCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: MediaFileImportSessionItemCrossRefId,
    val mediaFileId: MediaFileId,
    val importSessionItemId: ImportSessionItemId,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface MediaFileImportSessionItemCrossRefDao {
    @Query("select * from mediafileimportsessionitemcrossref")
    fun getAll(): Flow<List<MediaFileImportSessionItemCrossRef>>

    @Query("""
        insert into mediafileimportsessionitemcrossref (mediaFileId, importSessionItemId, creationDatetime, updateDatetime)
        values (:mediaFileId, :importSessionItemId, :creationDatetime, :updateDatetime)
    """)
    suspend fun add(
        mediaFileId: MediaFileId,
        importSessionItemId: ImportSessionItemId,
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Query("delete from mediafileimportsessionitemcrossref where mediaFileId = :mediaFileId and importSessionItemId = :importSessionItemId")
    suspend fun delete(mediaFileId: MediaFileId, importSessionItemId: ImportSessionItemId)
}