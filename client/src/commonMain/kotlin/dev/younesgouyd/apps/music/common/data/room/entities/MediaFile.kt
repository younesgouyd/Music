package dev.younesgouyd.apps.music.common.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.common.util.ImportSourceType
import kotlinx.coroutines.flow.Flow

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Track::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MediaFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val trackId: Long,
    val sourceUri: String,
    val sourceWebpageUrl: String?,
    val sourceType: ImportSourceType,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface MediaFileDao {
    @Query("select * from mediafile where trackId = :trackId")
    fun getTrackMediaFiles(trackId: Long): Flow<List<MediaFile>>

    @Query("""
        insert into mediafile (name, trackId, sourceUri, sourceWebpageUrl, sourceType, creationDatetime, updateDatetime)
        values (:name, :trackId, :sourceUri, :sourceWebpageUrl, :sourceType, :creationDatetime, :updateDatetime)
    """)
    suspend fun add(
        name: String,
        trackId: Long,
        sourceUri: String,
        sourceWebpageUrl: String?,
        sourceType: ImportSourceType,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long
}