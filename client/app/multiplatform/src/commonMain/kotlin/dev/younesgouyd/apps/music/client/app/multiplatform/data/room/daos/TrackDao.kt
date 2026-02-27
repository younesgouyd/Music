package dev.younesgouyd.apps.music.client.app.multiplatform.data.room.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import dev.younesgouyd.apps.music.client.app.multiplatform.data.*
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.PlaylistTrack
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.Track
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.TrackRelation
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TrackDao {
    @Transaction
    @Query("select * from track where id = :id")
    abstract fun get(id: TrackId): Flow<TrackRelation?>

    @Transaction
    @Query("""
        select distinct t.*
        from track t
        join importsessionitem i on i.id = t.importSessionItemId
        left join spotifytrack sp on sp.id = t.spotifyTrackId
        where t.id > :lastId
        and (
            (sp.name is not null and sp.name like :nameQuery)
            or (sp.name is null and i.title is not null and i.title like :nameQuery) 
        )
        order by t.id asc
        limit :limit
    """)
    abstract suspend fun search(
        nameQuery: String,
        limit: Int,
        lastId: TrackId
    ): List<TrackRelation>

    @Transaction
    @Query("""
        select distinct t.*
        from track t
        join importsessionitem i on i.id = t.importSessionItemId
        left join spotifytrack sp on sp.id = t.spotifyTrackId
        left join tagtrackcrossref cr on cr.trackId = t.id
        where t.id > :lastId
        and (
            (sp.name is not null and sp.name like :nameQuery)
            or (sp.name is null and i.title is not null and i.title like :nameQuery) 
        )
        and (
            cr.tagId in (:tags)
            or (:includeUntagged and cr.trackId is null)
        )
        order by t.id asc
        limit :limit
    """)
    abstract suspend fun search(
        nameQuery: String,
        tags: List<TagId>,
        includeUntagged: Boolean,
        limit: Int,
        lastId: TrackId
    ): List<TrackRelation>

    @Transaction
    @Query("select * from track where importSessionItemId = :id")
    abstract fun getImportSessionTrack(id: ImportSessionItemId): Flow<TrackRelation?>

    @Transaction
    @Query("""
        select distinct t.*
        from track t
        join importsessionitem i on i.id = t.importSessionItemId
        left join spotifytrack sp on sp.id = t.spotifyTrackId
        where folderId = :folderId
        and (
            (sp.name is not null and sp.name like :nameQuery)
            or (sp.name is null and i.title is not null and i.title like :nameQuery) 
        )
    """)
    abstract fun searchFolder(folderId: FolderId, nameQuery: String): Flow<List<TrackRelation>>

    @Transaction
    @Query("""
        select distinct t.*
        from track t
        join importsessionitem i on i.id = t.importSessionItemId
        left join spotifytrack sp on sp.id = t.spotifyTrackId
        left join tagtrackcrossref cr on cr.trackId = t.id
        where folderId = :folderId
        and (
            (sp.name is not null and sp.name like :nameQuery)
            or (sp.name is null and i.title is not null and i.title like :nameQuery) 
        )
        and (
            cr.tagId in (:tags)
            or (:includeUntagged and cr.trackId is null)
        )
    """)
    abstract fun searchFolder(
        folderId: FolderId,
        nameQuery: String,
        tags: List<TagId>,
        includeUntagged: Boolean
    ): Flow<List<TrackRelation>>

    @Transaction
    @Query("""
        select distinct t.*, cr.id as playlistTrackCrossRefId
        from track t
        join importsessionitem i on i.id = t.importSessionItemId
        left join spotifytrack sp on sp.id = t.spotifyTrackId
        join playlisttrackcrossref cr on cr.trackId = t.id
        where cr.playlistId = :id
        and (
            (sp.name is not null and sp.name like :nameQuery)
            or (sp.name is null and i.title is not null and i.title like :nameQuery) 
        )
    """)
    abstract fun searchPlaylist(id: PlaylistId, nameQuery: String): Flow<List<PlaylistTrack>>

    @Transaction
    @Query("""
        select distinct t.*
        from track t
        join importsessionitem i on i.id = t.importSessionItemId
        left join spotifytrack sp on sp.id = t.spotifyTrackId
        join tagtrackcrossref cr on cr.trackId = t.id
        where cr.tagId = :tag
        and (
            (sp.name is not null and sp.name like :nameQuery)
            or (sp.name is null and i.title is not null and i.title like :nameQuery) 
        )
    """)
    abstract fun searchWithTag(nameQuery: String, tag: TagId): Flow<List<TrackRelation>>

    @Query("select * from track where folderId = :id")
    abstract fun getFolderTracks(id: FolderId): Flow<List<Track>>

    @Transaction
    @Query("""
        select t.*
        from track t
        join spotifyartistspotifytrackcrossref cr on cr.spotifyTrackId = t.spotifyTrackId
        where cr.spotifyArtistId = :id
    """)
    abstract fun getArtistTracks(id: SpotifyArtistId): Flow<List<TrackRelation>>

    @Transaction
    @Query("""
        -- get tracks which are in albums that the artist does not own
        select t.*
        from track t
        join spotifytrack sp on sp.id = t.spotifyTrackId
        join spotifyartistspotifytrackcrossref cr on cr.spotifyTrackId = sp.id
        where t.id > :lastId
        and cr.spotifyArtistId = :id
        and cr.spotifyArtistId not in (
            select album_cr.spotifyArtistId
            from spotifyartistspotifyalbumcrossref album_cr 
            where album_cr.spotifyAlbumId = sp.spotifyAlbumId 
            and album_cr.spotifyArtistId = :id
        )
        and sp.name like :nameQuery
        order by t.id asc
        limit :limit
    """)
    abstract suspend fun searchArtistContributions(
        id: SpotifyArtistId,
        nameQuery: String,
        limit: Int,
        lastId: TrackId
    ): List<TrackRelation>

    @Transaction
    @Query("""
        select t.*
        from track t
        join spotifytrack sp on sp.id = t.spotifyTrackId
        where sp.spotifyAlbumId = :id
        order by sp.trackNumber
    """)
    abstract fun getAlbumTracks(id: SpotifyAlbumId): Flow<List<TrackRelation>>

    @Transaction
    @Query("""
        select t.*
        from track t
        join playlisttrackcrossref cr on cr.trackId = t.id
        where cr.playlistId = :id
        order by cr.position
    """)
    abstract fun getPlaylistTracks(id: PlaylistId): Flow<List<TrackRelation>>

    @Query("""
        select t.id
        from track t
        join spotifytrack sp on sp.id = t.spotifyTrackId
        where sp.spotifyId = :spotifyId
    """)
    abstract suspend fun getId(spotifyId: String): Long?

    @Query(
        """
        insert into track (importSessionItemId, spotifyTrackId, folderId, creationDatetime, updateDatetime)
        values (:importSessionItemId, :spotifyTrackId, :folderId, :creationDatetime, :updateDatetime)
    """
    )
    abstract suspend fun add(
        importSessionItemId: ImportSessionItemId,
        spotifyTrackId: SpotifyTrackId?,
        folderId: FolderId,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long


    @Query("update track set folderId = :folderId, updateDatetime = :updateDatetime where id = :id")
    abstract suspend fun updateFolderId(folderId: FolderId, updateDatetime: Long, id: TrackId)
}