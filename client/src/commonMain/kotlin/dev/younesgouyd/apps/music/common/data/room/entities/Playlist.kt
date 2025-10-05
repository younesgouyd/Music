package dev.younesgouyd.apps.music.common.data.room.entities

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val folderId: Long?,
    val image: ByteArray?,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface PlaylistDao {
    @Query("select * from playlist")
    fun getAll(): Flow<List<Playlist>>

    @Query("select * from playlist where id = :id")
    fun get(id: Long): Flow<Playlist>

    @Query("select * from playlist where folderId = :folderId")
    fun getFolderPlaylists(folderId: Long?): Flow<List<Playlist>>

    @Query("""
        select p.*
        from playlist p
        join playlisttrackcrossref cr on cr.playlistId = p.id
        where cr.trackId = :trackId
    """)
    fun getTrackPlaylists(trackId: Long): Flow<List<Playlist>>

    @Query("""
        insert into playlist (name, folderId, image, creationDatetime, updateDatetime)
        values (:name, :folderId, :image, :creationDatetime, :updateDatetime)
    """)
    suspend fun add(
        name: String,
        folderId: Long?,
        image: ByteArray?,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long

    @Query("update playlist set name = :name, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateName(name: String, updateDatetime: Long, id: Long)

    @Query("update playlist set folderId = :folderId, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateFolderId(folderId: Long?, updateDatetime: Long, id: Long)

    @Query("update playlist set image = :image, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateImage(image: ByteArray?, updateDatetime: Long, id: Long)

    @Query("delete from playlist where id = :id")
    suspend fun delete(id: Long)
}