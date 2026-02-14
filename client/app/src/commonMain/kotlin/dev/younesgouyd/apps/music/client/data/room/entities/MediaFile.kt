package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import dev.younesgouyd.apps.music.client.data.*
import kotlinx.coroutines.flow.Flow

@Entity
data class MediaFile(
    @PrimaryKey(autoGenerate = true)
    val id: MediaFileId,
    val fileName: String?,
    val creationDatetime: Long
)

@Dao
interface MediaFileDao {
    @Query("""
        select m.*
        from mediafile m
        join importsession i on i.imgId = m.id
        where i.id = :id
    """)
    fun getImportSessionImage(id: ImportSessionId): Flow<MediaFile?>

    @Query("""
        select m.*
        from mediafile m
        join importsessionitem i on i.imgId = m.id
        where i.id = :id
    """)
    fun getImportSessionItemImage(id: ImportSessionItemId): Flow<MediaFile?>

    @Query("""
        select m.*
        from mediafile m
        join importsessionitem i on i.audioFileId = m.id
        where i.id = :id
    """)
    fun getImportSessionItemAudio(id: ImportSessionItemId): Flow<MediaFile?>

    @Query("""
        select m.*
        from mediafile m
        join spotifyalbum a on a.largeImgId = m.id
        where a.id = :id
    """)
    fun getSpotifyAlbumImage(id: SpotifyAlbumId): Flow<MediaFile?>

    @Query("""
        select m.*
        from mediafile m
        join spotifyartist a on a.largeImgId = m.id
        where a.id = :id
    """)
    fun getSpotifyArtistImage(id: SpotifyArtistId): Flow<MediaFile?>

    @Query(
        """
        insert into mediafile (fileName, creationDatetime)
        values (:fileName, :creationDatetime)
    """
    )
    suspend fun add(
        fileName: String?,
        creationDatetime: Long
    ): Long
}