package dev.younesgouyd.apps.music.client.data.room.daos

import androidx.room.Dao
import androidx.room.Query
import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.ImportSessionId
import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSession
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ImportSessionDao {
    @Query("""
        select *
        from importsession
        order by creationDatetime desc
        limit :limit offset :offset
    """)
    abstract suspend fun getAll(limit: Int, offset: Int): List<ImportSession>

    @Query("select * from importsession where id = :id")
    abstract fun get(id: ImportSessionId): Flow<ImportSession?>

    @Query(
        """
        insert into importsession (uri, sourceType, inspection, destinationFolderId, imgId, creationDatetime)
        values (:uri, :sourceType, :inspection, :destinationFolderId, :imgId, :creationDatetime)
    """
    )
    abstract suspend fun add(
        uri: String,
        sourceType: ImportSession.SourceType,
        inspection: Inspection.ContainerInspection,
        destinationFolderId: FolderId,
        imgId: MediaFileId?,
        creationDatetime: Long
    ): Long
}