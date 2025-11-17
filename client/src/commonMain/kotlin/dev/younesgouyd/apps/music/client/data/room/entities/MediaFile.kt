package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import dev.younesgouyd.apps.music.client.data.*
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFile.Type
import kotlinx.coroutines.flow.Flow

@Entity
data class MediaFile(
    @PrimaryKey(autoGenerate = true)
    val id: MediaFileId,
    val type: Type,
    val creationDatetime: Long,
    val updateDatetime: Long
) {
    enum class Type {
        Audio, Video, Image
    }
}

@Dao
interface MediaFileDao {
    @Query("""
        select m.*
        from mediafile m
        join mediafiletrackcrossref cr on cr.mediaFileId = m.id
        where cr.trackId = :trackId
        and m.type = :type
    """)
    fun getTrackMediaFiles(trackId: TrackId, type: Type): Flow<List<MediaFile>>

    @Query("""
        select m.*
        from mediafile m
        join mediafileimportsessioncrossref cr on cr.mediaFileId = m.id
        where cr.importSessionId = :importSessionId
        and m.type = :type
    """)
    fun getImportSessionMediaFiles(importSessionId: ImportSessionId, type: Type): Flow<List<MediaFile>>

    @Query("""
        select m.*
        from mediafile m
        join mediafileimportsessionitemcrossref cr on cr.mediaFileId = m.id
        where cr.importSessionItemId = :importSessionItemId
        and m.type = :type
    """)
    fun getImportSessionItemMediaFiles(importSessionItemId: ImportSessionItemId, type: Type): Flow<List<MediaFile>>

    @Query("""
        select m.*
        from mediafile m
        join mediafileartistcrossref cr on cr.mediaFileId = m.id
        where cr.artistId = :artistId
        and m.type = :type
    """)
    fun getArtistMediaFiles(artistId: ArtistId, type: Type): Flow<List<MediaFile>>

    @Query("""
        select m.*
        from mediafile m
        join mediafileplaylistcrossref cr on cr.mediaFileId = m.id
        where cr.playlistId = :playlistId
        and m.type = :type
    """)
    fun getPlaylistMediaFiles(playlistId: PlaylistId, type: Type): Flow<List<MediaFile>>

    @Query(
        """
        insert into mediafile (type, creationDatetime, updateDatetime)
        values (:type, :creationDatetime, :updateDatetime)
    """
    )
    suspend fun add(
        type: Type,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long
}