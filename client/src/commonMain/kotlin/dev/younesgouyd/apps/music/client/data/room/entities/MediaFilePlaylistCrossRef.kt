package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*

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
data class MediaFilePlaylistCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val mediaFileId: Long,
    val playlistId: Long,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface MediaFilePlaylistCrossRefDao {
    @Query("""
        insert into mediafileplaylistcrossref (mediaFileId, playlistId, creationDatetime, updateDatetime)
        values (:mediaFileId, :playlistId, :creationDatetime, :updateDatetime)
    """)
    suspend fun add(mediaFileId: Long, playlistId: Long, creationDatetime: Long, updateDatetime: Long)


    @Query("delete from mediafileplaylistcrossref where mediaFileId = :mediaFileId and playlistId = :playlistId")
    suspend fun delete(mediaFileId: Long, playlistId: Long)
}