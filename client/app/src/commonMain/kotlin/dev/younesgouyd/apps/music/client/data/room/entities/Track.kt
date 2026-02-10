package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.*
import kotlinx.coroutines.flow.Flow

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ImportSessionItem::class,
            parentColumns = ["id"],
            childColumns = ["importSessionItemId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = SpotifyTrack::class,
            parentColumns = ["id"],
            childColumns = ["spotifyTrackId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT // TODO: if a track is deleted, also delete its SpotifyTrack if unused elsewhere, and clear its imports if unused
        )
    ],
    indices = [
        Index(value = ["importSessionItemId"], unique = true),
        Index(value = ["spotifyTrackId"], unique = true),
        Index(value = ["folderId"])
    ]
)
data class Track(
    @PrimaryKey(autoGenerate = true)
    val id: TrackId,
    val importSessionItemId: ImportSessionItemId,
    val spotifyTrackId: SpotifyTrackId?,
    val folderId: FolderId,
    val creationDatetime: Long,
    val updateDatetime: Long
)

data class TrackRelation(
    @Embedded val track: Track,
    @Relation(
        parentColumn = "importSessionItemId",
        entityColumn = "id"
    )
    val originalImport: ImportSessionItem,
    @Relation(
        parentColumn = "spotifyTrackId",
        entityColumn = "id"
    )
    val spotifyTrack: SpotifyTrack?
)

data class PlaylistTrack(
    @Embedded val track: Track,
    val playlistTrackCrossRefId: PlaylistTrackCrossRefId,
    @Relation(
        parentColumn = "importSessionItemId",
        entityColumn = "id"
    )
    val originalImport: ImportSessionItem,
    @Relation(
        parentColumn = "spotifyTrackId",
        entityColumn = "id"
    )
    val spotifyTrack: SpotifyTrack?,
    @Relation(
        parentColumn = "id",
        entityColumn = "trackId"
    )
    val playlistCrossRef: PlaylistTrackCrossRef?
)

@Dao
interface TrackDao {
    @Transaction
    @Query("select * from track")
    fun getAll(): Flow<List<TrackRelation>>

    @Transaction
    @Query("select * from track where id = :id")
    fun get(id: TrackId): Flow<TrackRelation>

    @Transaction
    @Query("""
        select distinct t.*
        from track t
        join importsessionitem i on i.id = t.importSessionItemId
        left join spotifytrack sp on sp.id = t.spotifyTrackId
        left join tagtrackcrossref cr on cr.trackId = t.id
        where (
            (sp.name is not null and sp.name like :nameQuery)
            or (sp.name is null and i.title is not null and i.title like :nameQuery) 
        )
        and (
            cr.tagId in (:tags)
            or (:includeUntagged and cr.trackId is null)
        )
    """)
    fun search(nameQuery: String, tags: List<TagId>, includeUntagged: Boolean): Flow<List<TrackRelation>>

    @Transaction
    @Query("select * from track where importSessionItemId = :id")
    fun getImportSessionTrack(id: ImportSessionItemId): Flow<TrackRelation?>

    fun searchFolder(folderId: FolderId, nameQuery: String, tags: List<TagId>, includeUntagged: Boolean): Flow<List<TrackRelation>> {
        return if (tags.isEmpty()) {
            searchFolder(folderId, nameQuery)
        } else {
            searchFolderWithTags(folderId, nameQuery, tags, includeUntagged)
        }
    }

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
    fun searchFolder(folderId: FolderId, nameQuery: String): Flow<List<TrackRelation>>

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
    fun searchFolderWithTags(
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
    fun searchPlaylist(id: PlaylistId, nameQuery: String): Flow<List<PlaylistTrack>>

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
    fun searchWithTag(nameQuery: String, tag: TagId): Flow<List<TrackRelation>>

    @Query("select * from track where folderId = :id")
    fun getFolderTracks(id: FolderId): Flow<List<Track>>

    @Transaction
    @Query("""
        select t.*
        from track t
        join spotifyartistspotifytrackcrossref cr on cr.spotifyTrackId = t.spotifyTrackId
        where cr.spotifyArtistId = :id
    """)
    fun getArtistTracks(id: SpotifyArtistId): Flow<List<TrackRelation>>

    @Transaction
    @Query("""
        -- get tracks which are in albums that the artist does not own
        select t.*
        from track t
        join spotifytrack sp on sp.id = t.spotifyTrackId
        join spotifyartistspotifytrackcrossref cr on cr.spotifyTrackId = sp.id
        where cr.spotifyArtistId = :id
        and cr.spotifyArtistId not in (
            select album_cr.spotifyArtistId
            from spotifyartistspotifyalbumcrossref album_cr 
            where album_cr.spotifyAlbumId = sp.spotifyAlbumId 
            and album_cr.spotifyArtistId = :id
        )
        and sp.name like :nameQuery
    """)
    fun searchArtistContributions(id: SpotifyArtistId, nameQuery: String): Flow<List<TrackRelation>>

    @Transaction
    @Query("""
        select t.*
        from track t
        join spotifytrack sp on sp.id = t.spotifyTrackId
        where sp.spotifyAlbumId = :id
        order by sp.trackNumber
    """)
    fun getAlbumTracks(id: SpotifyAlbumId): Flow<List<TrackRelation>>

    @Transaction
    @Query("""
        select t.*
        from track t
        join playlisttrackcrossref cr on cr.trackId = t.id
        where cr.playlistId = :id
        order by cr.position
    """)
    fun getPlaylistTracks(id: PlaylistId): Flow<List<TrackRelation>>

    @Query("""
        select t.id
        from track t
        join spotifytrack sp on sp.id = t.spotifyTrackId
        where sp.spotifyId = :spotifyId
    """)
    suspend fun getId(spotifyId: String): Long?

    @Query(
        """
        insert into track (importSessionItemId, spotifyTrackId, folderId, creationDatetime, updateDatetime)
        values (:importSessionItemId, :spotifyTrackId, :folderId, :creationDatetime, :updateDatetime)
    """
    )
    suspend fun add(
        importSessionItemId: ImportSessionItemId,
        spotifyTrackId: SpotifyTrackId?,
        folderId: FolderId,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long


    @Query("update track set folderId = :folderId, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateFolderId(folderId: FolderId, updateDatetime: Long, id: TrackId)
}