package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.FolderId
import kotlinx.coroutines.flow.Flow

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["parentFolderId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parentFolderId"])
    ]
)
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: FolderId,
    val name: String,
    val parentFolderId: FolderId?,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface FolderDao {
    @Query("select * from folder where id = :id")
    fun get(id: FolderId): Flow<Folder?>

    fun searchFolder(folderId: FolderId?, nameQuery: String): Flow<List<Folder>> {
        return if (folderId == null) {
            searchRootFolder(nameQuery)
        } else {
            searchFolder(folderId, nameQuery)
        }
    }

    @Query("select * from folder where parentFolderId is null and name like :nameQuery")
    fun searchRootFolder(nameQuery: String): Flow<List<Folder>>

    @Query("select * from folder where parentFolderId = :parentFolderId and name like :nameQuery")
    fun searchFolder(parentFolderId: FolderId, nameQuery: String): Flow<List<Folder>>

    @Query("select * from folder where parentFolderId = :parentFolderId")
    fun getSubfolders(parentFolderId: FolderId): Flow<List<Folder>>

    @Query("select * from folder where parentFolderId is null")
    fun getRoot(): Flow<List<Folder>>

    @Query(
        """
        insert into folder (name, parentFolderId, creationDatetime, updateDatetime)
        values (:name, :parentFolderId, :creationDatetime, :updateDatetime)
    """
    )
    suspend fun add(
        name: String,
        parentFolderId: FolderId?,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long

    @Query("update folder set name = :name, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateName(name: String, updateDatetime: Long, id: FolderId)

    @Query("update folder set parentFolderId = :parentFolderId, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateParentFolderId(parentFolderId: FolderId?, updateDatetime: Long, id: FolderId)

    @Query("delete from folder where id = :id")
    suspend fun delete(id: FolderId)
}