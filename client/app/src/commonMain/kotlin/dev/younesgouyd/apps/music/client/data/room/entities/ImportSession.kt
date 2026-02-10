package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.ImportSessionId
import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.flow.Flow

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["destinationFolderId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = MediaFile::class,
            parentColumns = ["id"],
            childColumns = ["imgId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["destinationFolderId"]),
        Index(value = ["imgId"], unique = true)
    ]
)
data class ImportSession(
    @PrimaryKey(autoGenerate = true)
    val id: ImportSessionId,
    val uri: String,
    val sourceType: SourceType,
    val inspection: Inspection.ContainerInspection,
    val destinationFolderId: FolderId,
    val imgId: MediaFileId?,
    val creationDatetime: Long
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

    @Query(
        """
        insert into importsession (uri, sourceType, inspection, destinationFolderId, imgId, creationDatetime)
        values (:uri, :sourceType, :inspection, :destinationFolderId, :imgId, :creationDatetime)
    """
    )
    suspend fun add(
        uri: String,
        sourceType: ImportSession.SourceType,
        inspection: Inspection.ContainerInspection,
        destinationFolderId: FolderId,
        imgId: MediaFileId?,
        creationDatetime: Long
    ): Long
}