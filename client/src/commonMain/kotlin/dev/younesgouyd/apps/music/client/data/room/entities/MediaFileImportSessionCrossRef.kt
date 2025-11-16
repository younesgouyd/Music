package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*

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
data class MediaFileImportSessionCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val mediaFileId: Long,
    val importSessionId: Long,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface MediaFileImportSessionCrossRefDao {
    @Query("""
        insert into mediafileimportsessioncrossref (mediaFileId, importSessionId, creationDatetime, updateDatetime)
        values (:mediaFileId, :importSessionId, :creationDatetime, :updateDatetime)
    """)
    suspend fun add(mediaFileId: Long, importSessionId: Long, creationDatetime: Long, updateDatetime: Long)


    @Query("delete from mediafileimportsessioncrossref where mediaFileId = :mediaFileId and importSessionId = :importSessionId")
    suspend fun delete(mediaFileId: Long, importSessionId: Long)
}