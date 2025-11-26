package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.ImportSessionId
import dev.younesgouyd.apps.music.client.data.PlaylistId
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ImportSession::class,
            parentColumns = ["id"],
            childColumns = ["importSessionId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.SET_NULL // TODO
        )
    ]
)
@Serializable
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: PlaylistId,
    val name: String,
    val folderId: FolderId?,
    val importSessionId: ImportSessionId?,
    val importUri: String?,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface PlaylistDao {
    @Query("select * from playlist")
    fun getAll(): Flow<List<Playlist>>

    @Query("select * from playlist where id = :id")
    fun get(id: PlaylistId): Flow<Playlist>

    @Query("select * from playlist where name like :nameQuery")
    fun search(nameQuery: String): Flow<List<Playlist>>

    fun searchFolder(folderId: FolderId?, nameQuery: String): Flow<List<Playlist>> {
        return if (folderId == null) {
            searchRootFolder(nameQuery)
        } else {
            searchFolder(folderId, nameQuery)
        }
    }

    @Query("select * from playlist where folderId is null and name like :nameQuery")
    fun searchRootFolder(nameQuery: String): Flow<List<Playlist>>

    @Query("select * from playlist where folderId = :folderId and name like :nameQuery")
    fun searchFolder(folderId: FolderId, nameQuery: String): Flow<List<Playlist>>

    @Query("select * from playlist where importSessionId = :importSessionId")
    fun getImportSessionPlaylist(importSessionId: ImportSessionId): Flow<Playlist?>

    @Query("select * from playlist where folderId = :folderId")
    fun getFolderPlaylists(folderId: FolderId?): Flow<List<Playlist>>

    @Query(
        """
        insert into playlist (name, folderId, importSessionId, importUri, creationDatetime, updateDatetime)
        values (:name, :folderId, :importSessionId, :importUri, :creationDatetime, :updateDatetime)
    """
    )
    suspend fun add(
        name: String,
        folderId: FolderId?,
        importSessionId: ImportSessionId?,
        importUri: String?,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long

    @Query("update playlist set name = :name, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateName(name: String, updateDatetime: Long, id: PlaylistId)

    @Query("update playlist set folderId = :folderId, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateFolderId(folderId: FolderId?, updateDatetime: Long, id: PlaylistId)

    @Query("delete from playlist where id = :id")
    suspend fun delete(id: PlaylistId)
}