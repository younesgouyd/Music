package dev.younesgouyd.apps.music.common.data.room.entities

import androidx.room.*

@Entity(
    indices = [
        Index(value = ["artistId", "trackId"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Artist::class,
            parentColumns = ["id"],
            childColumns = ["artistId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Track::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ArtistTrackCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val artistId: Long,
    val trackId: Long,
    val creationDatetime: Long,
    val updateDatetime: Long
)

@Dao
interface ArtistTrackCrossRefDao {
    @Query("""
        insert into artisttrackcrossref (artistId, trackId, creationDatetime, updateDatetime)
        values (:artistId, :trackId, :creationDatetime, :updateDatetime)
    """)
    suspend fun add(artistId: Long, trackId: Long, creationDatetime: Long, updateDatetime: Long)

    @Query("delete from artisttrackcrossref where artistId = :artistId and trackId = :trackId")
    suspend fun delete(artistId: Long, trackId: Long)
}