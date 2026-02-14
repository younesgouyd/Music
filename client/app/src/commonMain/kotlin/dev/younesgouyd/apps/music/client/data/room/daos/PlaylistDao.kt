package dev.younesgouyd.apps.music.client.data.room.daos

import androidx.room.Dao
import androidx.room.Query
import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.PlaylistId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.room.entities.Playlist
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlaylistDao {
    @Query("select * from playlist")
    abstract fun getAll(): Flow<List<Playlist>>

    @Query("select * from playlist where id = :id")
    abstract fun get(id: PlaylistId): Flow<Playlist?>

    @Query("select * from playlist where name like :nameQuery")
    abstract fun search(nameQuery: String): Flow<List<Playlist>>

    fun searchFolder(folderId: FolderId?, nameQuery: String): Flow<List<Playlist>> {
        return if (folderId == null) {
            searchRootFolder(nameQuery)
        } else {
            searchFolder(folderId, nameQuery)
        }
    }

    @Query("select * from playlist where folderId is null and name like :nameQuery")
    abstract fun searchRootFolder(nameQuery: String): Flow<List<Playlist>>

    @Query("select * from playlist where folderId = :folderId and name like :nameQuery")
    abstract fun searchFolder(folderId: FolderId, nameQuery: String): Flow<List<Playlist>>

    @Query("select * from playlist where folderId = :folderId")
    abstract fun getFolderPlaylists(folderId: FolderId?): Flow<List<Playlist>>

    @Query("""
        select p.*
        from playlist p
        join playlisttrackcrossref cr on cr.playlistId = p.id
        where cr.trackId = :id
    """)
    abstract fun getTrackPlaylists(id: TrackId): Flow<List<Playlist>>

    @Query(
        """
        insert into playlist (name, folderId, creationDatetime, updateDatetime)
        values (:name, :folderId, :creationDatetime, :updateDatetime)
    """
    )
    abstract suspend fun add(
        name: String,
        folderId: FolderId?,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long

    @Query("update playlist set name = :name, updateDatetime = :updateDatetime where id = :id")
    abstract suspend fun updateName(name: String, updateDatetime: Long, id: PlaylistId)

    @Query("update playlist set folderId = :folderId, updateDatetime = :updateDatetime where id = :id")
    abstract suspend fun updateFolderId(folderId: FolderId?, updateDatetime: Long, id: PlaylistId)

    @Query("delete from playlist where id = :id")
    abstract suspend fun delete(id: PlaylistId)
}