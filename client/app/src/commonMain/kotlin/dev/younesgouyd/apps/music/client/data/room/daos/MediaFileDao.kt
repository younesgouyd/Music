package dev.younesgouyd.apps.music.client.data.room.daos

import androidx.room.Dao
import androidx.room.Query
import dev.younesgouyd.apps.music.client.data.ImportSessionId
import dev.younesgouyd.apps.music.client.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.data.SpotifyAlbumId
import dev.younesgouyd.apps.music.client.data.SpotifyArtistId
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFile
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MediaFileDao {
    @Query("""
        select m.*
        from mediafile m
        join importsession i on i.imgId = m.id
        where i.id = :id
    """)
    abstract fun getImportSessionImage(id: ImportSessionId): Flow<MediaFile?>

    @Query("""
        select m.*
        from mediafile m
        join importsessionitem i on i.imgId = m.id
        where i.id = :id
    """)
    abstract fun getImportSessionItemImage(id: ImportSessionItemId): Flow<MediaFile?>

    @Query("""
        select m.*
        from mediafile m
        join importsessionitem i on i.audioFileId = m.id
        where i.id = :id
    """)
    abstract fun getImportSessionItemAudio(id: ImportSessionItemId): Flow<MediaFile?>

    @Query("""
        select m.*
        from mediafile m
        join spotifyalbum a on a.largeImgId = m.id
        where a.id = :id
    """)
    abstract fun getSpotifyAlbumImage(id: SpotifyAlbumId): Flow<MediaFile?>

    @Query("""
        select m.*
        from mediafile m
        join spotifyartist a on a.largeImgId = m.id
        where a.id = :id
    """)
    abstract fun getSpotifyArtistImage(id: SpotifyArtistId): Flow<MediaFile?>

    @Query(
        """
        insert into mediafile (fileName, creationDatetime)
        values (:fileName, :creationDatetime)
    """
    )
    abstract suspend fun add(
        fileName: String?,
        creationDatetime: Long
    ): Long
}