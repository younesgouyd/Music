package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.SpotifyAlbumId
import dev.younesgouyd.apps.music.client.data.SpotifyArtistId
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
data class SpotifyAlbum(
    @PrimaryKey
    val id: SpotifyAlbumId,
    val spotifyId: String,
    val name: String,
    val albumType: String,
    val releaseDate: String,
    val releaseDatePrecision: String,
    val smallImgId: MediaFileId,
    val mediumImgId: MediaFileId,
    val largeImgId: MediaFileId,
    val apiResponse: String,
    val creationDatetime: Long
)

@Dao
interface SpotifyAlbumDao {
    @Query("select * from spotifyalbum where id = :id")
    fun get(id: SpotifyAlbumId): Flow<SpotifyAlbum>

    @Query("""
        select a.*
        from spotifyalbum a
        join spotifyartistspotifyalbumcrossref cr on cr.spotifyAlbumId = a.id
        where cr.spotifyArtistId = :id
        and a.name like :nameQuery
    """)
    fun searchArtist(id: SpotifyArtistId, nameQuery: String): Flow<List<SpotifyAlbum>>
}