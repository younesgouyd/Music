package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.PlaylistId
import dev.younesgouyd.apps.music.client.data.PlaylistTrackCrossRefId
import dev.younesgouyd.apps.music.client.data.TrackId
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity(
    indices = [
        Index(value = ["playlistId", "trackId"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
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
data class PlaylistTrackCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: PlaylistTrackCrossRefId,
    val playlistId: PlaylistId,
    val trackId: TrackId,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface PlaylistTrackCrossRefDao {
    @Query("select * from playlisttrackcrossref")
    fun getAll(): Flow<List<PlaylistTrackCrossRef>>

    @Query(
        """
        select *
        from playlisttrackcrossref
        where playlistId = :playlistId
        and trackId = :trackId
    """
    )
    fun get(playlistId: PlaylistId, trackId: TrackId): Flow<PlaylistTrackCrossRef?>

    @Query(
        """
        insert into playlisttrackcrossref (playlistId, trackId, creationDatetime, updateDatetime)
        values (:playlistId, :trackId, :creationDatetime, :updateDatetime)
    """
    )
    suspend fun add(
        playlistId: PlaylistId,
        trackId: TrackId,
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Query("delete from playlisttrackcrossref where playlistId = :playlistId and trackId = :trackId")
    suspend fun delete(playlistId: PlaylistId, trackId: TrackId)
}