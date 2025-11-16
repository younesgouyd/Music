package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*

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
data class MediaFileImportSessionItemCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val mediaFileId: Long,
    val importSessionItemId: Long,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface MediaFileImportSessionItemCrossRefDao {
    @Query("""
        insert into mediafileimportsessionitemcrossref (mediaFileId, importSessionItemId, creationDatetime, updateDatetime)
        values (:mediaFileId, :importSessionItemId, :creationDatetime, :updateDatetime)
    """)
    suspend fun add(mediaFileId: Long, importSessionItemId: Long, creationDatetime: Long, updateDatetime: Long)

    @Query("delete from mediafileimportsessionitemcrossref where mediaFileId = :mediaFileId and importSessionItemId = :importSessionItemId")
    suspend fun delete(mediaFileId: Long, importSessionItemId: Long)
}