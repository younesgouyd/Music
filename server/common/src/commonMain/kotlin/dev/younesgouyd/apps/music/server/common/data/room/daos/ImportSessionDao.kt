package dev.younesgouyd.apps.music.server.common.data.room.daos

import androidx.room.Dao
import androidx.room.Query
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.server.common.data.room.entities.ImportSession
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
}