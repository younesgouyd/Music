package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.PlaylistId
import dev.younesgouyd.apps.music.client.data.PlaylistTrackCrossRefId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.room.PlaylistTrackView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

@Entity(
    indices = [
        Index(value = ["playlistId", "trackId"], unique = true),
        Index(value = ["playlistId", "position"], unique = true)
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
    val position: Int,
    val creationDatetime: Long,
    val updateDatetime: Long
)

private val playlistLocks = ConcurrentHashMap<PlaylistId, Mutex>()

private fun lockFor(id: PlaylistId): Mutex {
    return playlistLocks.computeIfAbsent(id) { Mutex() }
}

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

    suspend fun addWithAutoPosition(
        playlistId: PlaylistId,
        trackId: TrackId,
        creationDatetime: Long,
        updateDatetime: Long
    ) {
        lockFor(playlistId).withLock {
            add(
                playlistId = playlistId,
                trackId = trackId,
                position = nextPosition(playlistId),
                creationDatetime = creationDatetime,
                updateDatetime = updateDatetime
            )
        }
    }

    @Query(
        """
        insert into playlisttrackcrossref (playlistId, trackId, position, creationDatetime, updateDatetime)
        values (:playlistId, :trackId, :position, :creationDatetime, :updateDatetime)
    """
    )
    suspend fun add(
        playlistId: PlaylistId,
        trackId: TrackId,
        position: Int,
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Transaction
    suspend fun changeItemPosition(playlistId: PlaylistId, from: Int, to: Int) {
        require(from != to)
        lockFor(playlistId).withLock {
            val temp = -1
            updatePosition(
                playlistId = playlistId,
                oldPosition = from,
                newPosition = temp
            )
            putItemsInATemporaryPosition(
                playlistId = playlistId,
                from = from,
                to = to
            )
            shiftItems(
                playlistId = playlistId,
                from = from,
                to = to
            )
            updatePosition(
                playlistId = playlistId,
                oldPosition = temp,
                newPosition = to
            )
        }
    }

    @Query("delete from playlisttrackcrossref where playlistId = :playlistId and trackId = :trackId")
    suspend fun delete(playlistId: PlaylistId, trackId: TrackId)

    @Query("""
        select COALESCE(MAX(position), -1) + 1
        from playlisttrackcrossref
        where playlistId = :playlistId
    """)
    suspend fun nextPosition(playlistId: PlaylistId): Int

    @Query("""
        update playlisttrackcrossref
        set position = :newPosition
        where playlistId = :playlistId
        and position = :oldPosition
    """)
    suspend fun updatePosition(
        playlistId: PlaylistId,
        oldPosition: Int,
        newPosition: Int
    )

    @Query("""
        update playlisttrackcrossref
        set position = -(position + 2)
        where playlistId = :playlistId
        and position between MIN(:from, :to) and MAX(:from, :to)
        and position != :from
    """)
    suspend fun putItemsInATemporaryPosition(
        playlistId: PlaylistId,
        from: Int,
        to: Int
    )

    @Query("""
        update playlisttrackcrossref
        set position =
            case
                when :from < :to then -position - 2 - 1
                else -position - 2 + 1
            end
        where playlistId = :playlistId
        and position <= -2
    """)
    suspend fun shiftItems(
        playlistId: PlaylistId,
        from: Int,
        to: Int
    )
}

@Dao
interface PlaylistTrackViewDao {
    @Query(
        """
        select *
        from playlist_track_view
        where playlistId = :id
        and name like :nameQuery
        order by position
    """
    )
    fun search(id: PlaylistId, nameQuery: String): Flow<List<PlaylistTrackView>>
}