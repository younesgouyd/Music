package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.ImportSessionId
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["destinationFolderId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
@Serializable
data class ImportSession(
    @PrimaryKey(autoGenerate = true)
    val id: ImportSessionId,
    val uri: String,
    val sourceType: SourceType,
    val inspection: Inspection.ContainerInspection,
    val destinationFolderId: FolderId?,
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
    fun get(id: ImportSessionId): Flow<ImportSession>
}