package dev.younesgouyd.apps.music.client.app.multiplatform.data.room.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import dev.younesgouyd.apps.music.client.app.multiplatform.data.PlaylistId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.TrackId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.PlaylistTrackCrossRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

@Dao
abstract class PlaylistTrackCrossRefDao {
    companion object {
        private val playlistLocks = ConcurrentHashMap<PlaylistId, Mutex>()

        private fun lockFor(id: PlaylistId): Mutex {
            return playlistLocks.computeIfAbsent(id) { Mutex() }
        }
    }
    
    @Query(
        """
        select *
        from playlisttrackcrossref
        where playlistId = :playlistId
        and trackId = :trackId
    """
    )
    abstract fun get(playlistId: PlaylistId, trackId: TrackId): Flow<PlaylistTrackCrossRef?>

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
    abstract suspend fun add(
        playlistId: PlaylistId,
        trackId: TrackId,
        position: Int,
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Transaction
    open suspend fun changeItemPosition(playlistId: PlaylistId, from: Int, to: Int) {
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
    abstract suspend fun delete(playlistId: PlaylistId, trackId: TrackId)

    @Query("""
        select COALESCE(MAX(position), -1) + 1
        from playlisttrackcrossref
        where playlistId = :playlistId
    """)
    abstract suspend fun nextPosition(playlistId: PlaylistId): Int

    @Query("""
        update playlisttrackcrossref
        set position = :newPosition
        where playlistId = :playlistId
        and position = :oldPosition
    """)
    abstract suspend fun updatePosition(
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
    abstract suspend fun putItemsInATemporaryPosition(
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
    abstract suspend fun shiftItems(
        playlistId: PlaylistId,
        from: Int,
        to: Int
    )
}