package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.ArtistId
import dev.younesgouyd.apps.music.client.data.ArtistTrackCrossRefId
import dev.younesgouyd.apps.music.client.data.TrackId
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity(
    indices = [
        Index(value = ["artistId", "trackId"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Artist::class,
            parentColumns = ["id"],
            childColumns = ["artistId"],
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
data class ArtistTrackCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: ArtistTrackCrossRefId,
    val artistId: ArtistId,
    val trackId: TrackId,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface ArtistTrackCrossRefDao {
    @Query("select * from artisttrackcrossref")
    fun getAll(): Flow<List<ArtistTrackCrossRef>>

    @Query(
        """
        insert into artisttrackcrossref (artistId, trackId, creationDatetime, updateDatetime)
        values (:artistId, :trackId, :creationDatetime, :updateDatetime)
    """
    )
    suspend fun add(artistId: ArtistId, trackId: TrackId, creationDatetime: Long, updateDatetime: Long)

    @Query("delete from artisttrackcrossref where artistId = :artistId and trackId = :trackId")
    suspend fun delete(artistId: ArtistId, trackId: TrackId)
}