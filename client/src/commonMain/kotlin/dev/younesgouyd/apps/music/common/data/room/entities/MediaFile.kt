package dev.younesgouyd.apps.music.common.data.room.entities

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Track::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ImportSessionItem::class,
            parentColumns = ["id"],
            childColumns = ["importSessionItemId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class MediaFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val trackId: Long,
    val importSessionItemId: Long,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface MediaFileDao {
    @Query("select * from mediafile where trackId = :trackId")
    fun getTrackMediaFiles(trackId: Long): Flow<List<MediaFile>>

    @Query(
        """
        insert into mediafile (trackId, importSessionItemId, creationDatetime, updateDatetime)
        values (:trackId, :importSessionItemId, :creationDatetime, :updateDatetime)
    """
    )
    suspend fun add(
        trackId: Long,
        importSessionItemId: Long,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long
}