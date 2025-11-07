package dev.younesgouyd.apps.music.common.data.room.entities

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.flow.Flow

@Entity
data class ImportSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val uri: String,
    val sourceType: SourceType,
    val inspection: Inspection.ContainerInspection,
    val creationDatetime: Long,
    val updateDatetime: Long
) {
    enum class SourceType {
        Local,
        Internet
    }
}

@Dao
interface ImportSessionDao {
    @Query("select * from importsession order by creationDatetime desc")
    fun getAll(): Flow<List<ImportSession>>

    @Query("select * from importsession where id = :id")
    fun get(id: Long): Flow<ImportSession>
}