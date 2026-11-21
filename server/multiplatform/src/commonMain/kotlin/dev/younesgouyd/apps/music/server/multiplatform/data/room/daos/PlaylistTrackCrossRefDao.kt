package dev.younesgouyd.apps.music.server.multiplatform.data.room.daos

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import dev.younesgouyd.apps.music.common.models.PlaylistId
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.PlaylistTrackCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlaylistTrackCrossRefDao {
    @Query(
        """
        select *
        from playlisttrackcrossref
        where playlistId = :playlistId
        and trackId = :trackId
    """
    )
    abstract fun get(playlistId: PlaylistId, trackId: TrackId): Flow<PlaylistTrackCrossRef?>

    @Query(
        """
        insert into playlisttrackcrossref (playlistId, trackId, position, creationDatetime, updateDatetime)
        values (
            :playlistId,
            :trackId,
            (select coalesce(max(position), -1) + 1 from playlisttrackcrossref where playlistId = :playlistId),
            :creationDatetime,
            :updateDatetime
        )
    """
    )
    abstract suspend fun add(
        playlistId: PlaylistId,
        trackId: TrackId,
        creationDatetime: Long,
        updateDatetime: Long
    )

    @Query("""
        update playlisttrackcrossref
        set position = case
            when position = :from then :to
            when :from < :to then position - 1
            else position + 1
        end
        where playlistId = :playlistId
        and position between min(:from, :to) and max(:from, :to)
    """)
    abstract suspend fun changeItemPosition(playlistId: PlaylistId, from: Int, to: Int): Int

    @Transaction
    open suspend fun changeItemPosition2(playlistId: PlaylistId, from: Int, to: Int) {
        require(from != to)
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

    @Query("delete from playlisttrackcrossref where playlistId = :playlistId and trackId = :trackId")
    abstract suspend fun delete(playlistId: PlaylistId, trackId: TrackId)

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