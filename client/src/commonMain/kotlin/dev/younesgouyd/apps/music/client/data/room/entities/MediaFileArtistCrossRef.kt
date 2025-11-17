package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.ArtistId
import dev.younesgouyd.apps.music.client.data.MediaFileArtistCrossRefId
import dev.younesgouyd.apps.music.client.data.MediaFileId

@Entity(
    indices = [
        Index(value = ["mediaFileId", "artistId"], unique = true),
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
            entity = Artist::class,
            parentColumns = ["id"],
            childColumns = ["artistId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MediaFileArtistCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: MediaFileArtistCrossRefId,
    val mediaFileId: MediaFileId,
    val artistId: ArtistId,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface MediaFileArtistCrossRefDao {
    @Query("""
        insert into mediafileartistcrossref (mediaFileId, artistId, creationDatetime, updateDatetime)
        values (:mediaFileId, :artistId, :creationDatetime, :updateDatetime)
    """)
    suspend fun add(mediaFileId: MediaFileId, artistId: ArtistId, creationDatetime: Long, updateDatetime: Long)


    @Query("delete from mediafileartistcrossref where mediaFileId = :mediaFileId and artistId = :artistId")
    suspend fun delete(mediaFileId: MediaFileId, artistId: ArtistId)
}