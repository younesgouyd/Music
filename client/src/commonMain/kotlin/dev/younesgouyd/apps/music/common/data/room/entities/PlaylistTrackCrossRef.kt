package dev.younesgouyd.apps.music.common.data.room.entities

import androidx.room.*
import kotlinx.coroutines.flow.Flow

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
            entity = Artist::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistTrackCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val playlistId: Long,
    val trackId: Long,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface PlaylistTrackCrossRefDao {
    @Query("""
        select *
        from playlisttrackcrossref
        where playlistId = :playlistId
        and trackId = :trackId
    """)
    fun get(playlistId: Long, trackId: Long): Flow<PlaylistTrackCrossRef?>

    @Query("""
        insert into playlisttrackcrossref (playlistId, trackId, creationDatetime, updateDatetime)
        values (:playlistId, :trackId, :creationDatetime, :updateDatetime)
    """)
    suspend fun add(playlistId: Long, trackId: Long, creationDatetime: Long, updateDatetime: Long)

    @Query("delete from playlisttrackcrossref where playlistId = :playlistId and trackId = :trackId")
    suspend fun delete(playlistId: Long, trackId: Long)
}