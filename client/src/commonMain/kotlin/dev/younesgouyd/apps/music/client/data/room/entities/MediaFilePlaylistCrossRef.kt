package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.MediaFilePlaylistCrossRefId
import dev.younesgouyd.apps.music.client.data.PlaylistId
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity(
    indices = [
        Index(value = ["mediaFileId", "playlistId"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = MediaFile::class,
            parentColumns = ["id"],
            childColumns = ["mediaFileId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Serializable
data class MediaFilePlaylistCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: MediaFilePlaylistCrossRefId,
    val mediaFileId: MediaFileId,
    val playlistId: PlaylistId,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface MediaFilePlaylistCrossRefDao {
    @Query("select * from mediafileplaylistcrossref")
    fun getAll(): Flow<List<MediaFilePlaylistCrossRef>>

    @Query("""
        insert into mediafileplaylistcrossref (mediaFileId, playlistId, creationDatetime, updateDatetime)
        values (:mediaFileId, :playlistId, :creationDatetime, :updateDatetime)
    """)
    suspend fun add(
        mediaFileId: MediaFileId,
        playlistId: PlaylistId,
        creationDatetime: Long,
        updateDatetime: Long
    )


    @Query("delete from mediafileplaylistcrossref where mediaFileId = :mediaFileId and playlistId = :playlistId")
    suspend fun delete(mediaFileId: MediaFileId, playlistId: PlaylistId)
}