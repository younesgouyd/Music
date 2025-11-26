package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.MediaFileTrackCrossRefId
import dev.younesgouyd.apps.music.client.data.TrackId
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

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
@Serializable
data class MediaFileTrackCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: MediaFileTrackCrossRefId,
    val mediaFileId: MediaFileId,
    val trackId: TrackId,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface MediaFileTrackCrossRefDao {
    @Query("select * from mediafiletrackcrossref")
    fun getAll(): Flow<List<MediaFileTrackCrossRef>>

    @Query("""
        insert into mediafiletrackcrossref (mediaFileId, trackId, creationDatetime, updateDatetime)
        values (:mediaFileId, :trackId, :creationDatetime, :updateDatetime)
    """)
    suspend fun add(
        mediaFileId: MediaFileId,
        trackId: TrackId,
        creationDatetime: Long,
        updateDatetime: Long
    )


    @Query("delete from mediafiletrackcrossref where mediaFileId = :mediaFileId and trackId = :trackId")
    suspend fun delete(mediaFileId: MediaFileId, trackId: TrackId)
}