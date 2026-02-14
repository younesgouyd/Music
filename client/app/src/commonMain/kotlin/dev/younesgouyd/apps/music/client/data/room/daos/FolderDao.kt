package dev.younesgouyd.apps.music.client.data.room.daos

import androidx.room.Dao
import androidx.room.Query
import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.room.entities.Folder
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FolderDao {
    @Query("select * from folder where id = :id")
    abstract fun get(id: FolderId): Flow<Folder?>

    fun searchFolder(folderId: FolderId?, nameQuery: String): Flow<List<Folder>> {
        return if (folderId == null) {
            searchRootFolder(nameQuery)
        } else {
            searchFolder(folderId, nameQuery)
        }
    }

    @Query("select * from folder where parentFolderId is null and name like :nameQuery")
    abstract fun searchRootFolder(nameQuery: String): Flow<List<Folder>>

    @Query("select * from folder where parentFolderId = :parentFolderId and name like :nameQuery")
    abstract fun searchFolder(parentFolderId: FolderId, nameQuery: String): Flow<List<Folder>>

    @Query("select * from folder where parentFolderId = :parentFolderId")
    abstract fun getSubfolders(parentFolderId: FolderId): Flow<List<Folder>>

    @Query("select * from folder where parentFolderId is null")
    abstract fun getRoot(): Flow<List<Folder>>

    @Query(
        """
        insert into folder (name, parentFolderId, creationDatetime, updateDatetime)
        values (:name, :parentFolderId, :creationDatetime, :updateDatetime)
    """
    )
    abstract suspend fun add(
        name: String,
        parentFolderId: FolderId?,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long

    @Query("update folder set name = :name, updateDatetime = :updateDatetime where id = :id")
    abstract suspend fun updateName(name: String, updateDatetime: Long, id: FolderId)

    @Query("update folder set parentFolderId = :parentFolderId, updateDatetime = :updateDatetime where id = :id")
    abstract suspend fun updateParentFolderId(parentFolderId: FolderId?, updateDatetime: Long, id: FolderId)

    @Query("delete from folder where id = :id")
    abstract suspend fun delete(id: FolderId)
}