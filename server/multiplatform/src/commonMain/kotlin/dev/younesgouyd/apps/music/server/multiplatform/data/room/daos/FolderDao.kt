package dev.younesgouyd.apps.music.server.multiplatform.data.room.daos

import androidx.room3.Dao
import androidx.room3.Query
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.Folder
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

    @Query("select * from folder where parentFolderId is null and name like '%' || :nameQuery || '%' order by name asc")
    abstract fun searchRootFolder(nameQuery: String): Flow<List<Folder>>

    @Query("select * from folder where parentFolderId = :parentFolderId and name like '%' || :nameQuery || '%' order by name asc")
    abstract fun searchFolder(parentFolderId: FolderId, nameQuery: String): Flow<List<Folder>>

    @Query("select * from folder where parentFolderId = :parentFolderId order by name asc")
    abstract fun getSubfolders(parentFolderId: FolderId): Flow<List<Folder>>

    @Query("select * from folder where parentFolderId is null order by name asc")
    abstract fun getRoot(): Flow<List<Folder>>

    @Query("""
        with recursive ancestors(id, parentFolderId, depth) as (
            select id, parentFolderId, 0 from folder where id = :target
            union
            select f.id, f.parentFolderId, a.depth + 1
            from folder f
            join ancestors a on f.id = a.parentFolderId
        )
        select folder.*
        from ancestors
        join folder on folder.id = ancestors.id
        order by ancestors.depth desc
    """)
    abstract fun getPath(target: FolderId): Flow<List<Folder>>

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