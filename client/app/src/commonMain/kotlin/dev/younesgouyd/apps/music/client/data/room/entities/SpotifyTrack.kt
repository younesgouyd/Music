package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.SpotifyAlbumId
import dev.younesgouyd.apps.music.client.data.SpotifyTrackId
import kotlinx.coroutines.flow.Flow

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = SpotifyAlbum::class,
            parentColumns = ["id"],
            childColumns = ["spotifyAlbumId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["spotifyId"], unique = true),
        Index(value = ["spotifyAlbumId", "discNumber", "trackNumber"], unique = true)
    ]
)
data class SpotifyTrack(
    @PrimaryKey(autoGenerate = true)
    val id: SpotifyTrackId,
    val spotifyId: String, // TODO: (the same spotify track may have multiple ids because there's a copy for each market)
    val name: String,
    val spotifyAlbumId: SpotifyAlbumId,
    val discNumber: Int?,
    val trackNumber: Int?,
    val durationMs: Int?,
    val explicit: Boolean?,
    val apiResponse: String,
    val creationDatetime: Long
)

data class SpotifyTrackRelation(
    @Embedded val spotifyTrack: SpotifyTrack,
    @Relation(
        parentColumn = "id",
        entityColumn = "spotifyTrackId"
    )
    val track: Track?
)

@Dao
interface SpotifyTrackDao {
    @Query("select id from spotifytrack where spotifyId = :spotifyId")
    suspend fun getId(spotifyId: String): Long?

    @Transaction
    @Query("select * from spotifytrack where spotifyAlbumId = :id order by discNumber, trackNumber")
    fun getAlbumTracks(id: SpotifyAlbumId): Flow<List<SpotifyTrackRelation>>
}