package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*

@Entity(
    indices = [
        Index(value = ["mediaFileId", "trackId"], unique = true),
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
            entity = Track::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MediaFileTrackCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val mediaFileId: Long,
    val trackId: Long,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface MediaFileTrackCrossRefDao {
    @Query("""
        insert into mediafiletrackcrossref (mediaFileId, trackId, creationDatetime, updateDatetime)
        values (:mediaFileId, :trackId, :creationDatetime, :updateDatetime)
    """)
    suspend fun add(mediaFileId: Long, trackId: Long, creationDatetime: Long, updateDatetime: Long)


    @Query("delete from mediafiletrackcrossref where mediaFileId = :mediaFileId and trackId = :trackId")
    suspend fun delete(mediaFileId: Long, trackId: Long)
}