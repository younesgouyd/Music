package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.SpotifyAlbumId
import dev.younesgouyd.apps.music.client.data.SpotifyArtistId
import dev.younesgouyd.apps.music.client.data.SpotifyTrackId
import kotlinx.coroutines.flow.Flow

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = MediaFile::class,
            parentColumns = ["id"],
            childColumns = ["smallImgId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = MediaFile::class,
            parentColumns = ["id"],
            childColumns = ["mediumImgId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = MediaFile::class,
            parentColumns = ["id"],
            childColumns = ["largeImgId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["spotifyId"], unique = true),
        Index(value = ["smallImgId"], unique = true),
        Index(value = ["mediumImgId"], unique = true),
        Index(value = ["largeImgId"], unique = true)
    ]
)
data class SpotifyArtist(
    @PrimaryKey
    val id: SpotifyArtistId,
    val spotifyId: String,
    val name: String,
    val smallImgId: MediaFileId,
    val mediumImgId: MediaFileId,
    val largeImgId: MediaFileId,
    val apiResponse: String,
    val creationDatetime: Long
)

@Dao
interface SpotifyArtistDao {
    @Query("select * from spotifyartist where id = :id")
    fun get(id: SpotifyArtistId): Flow<SpotifyArtist?>

    @Query("select * from spotifyartist where name like :nameQuery")
    fun search(nameQuery: String): Flow<List<SpotifyArtist>>

    @Query("""
        select a.*
        from spotifyartist a
        join spotifyartistspotifytrackcrossref cr on cr.spotifyArtistId = a.id
        where cr.spotifyTrackId = :id
    """)
    fun getSpotifyTrackSpotifyArtists(id: SpotifyTrackId): Flow<List<SpotifyArtist>>

    @Query("""
        select a.*
        from spotifyartist a
        join spotifyartistspotifyalbumcrossref cr on cr.spotifyArtistId = a.id
        where cr.spotifyAlbumId = :id
    """)
    fun getSpotifyAlbumSpotifyArtists(id: SpotifyAlbumId): Flow<List<SpotifyArtist>>
}