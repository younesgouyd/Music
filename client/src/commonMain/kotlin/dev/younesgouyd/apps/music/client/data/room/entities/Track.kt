package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Serializable
data class Track(
    @PrimaryKey(autoGenerate = true)
    val id: TrackId,
    val name: String,
    val folderId: FolderId?,
    val album: String?,
    val lyrics: String?,
    val albumTrackNumber: Int?,
    val durationMillis: Long?,
    val creationDatetime: Long,
    val updateDatetime: Long
) {
    val duration: Duration? get() = durationMillis?.milliseconds
}

@Dao
interface TrackDao {
    @Query("select * from track")
    fun getAll(): Flow<List<Track>>

    @Query("select * from track where id = :id")
    fun get(id: TrackId): Flow<Track>

    fun search(nameQuery: String, tags: List<TagId>, includeUntagged: Boolean): Flow<List<Track>> {
        return if (tags.isEmpty()) {
            search(nameQuery)
        } else {
            searchWithTags(nameQuery, tags, includeUntagged)
        }
    }

    @Query("select * from track where name like :nameQuery")
    fun search(nameQuery: String): Flow<List<Track>>

    @Query("""
        select distinct t.*
        from track t
        left join tagtrackcrossref cr on cr.trackId = t.id
        where (
            cr.tagId in (:tags)
            or (:includeUntagged and cr.trackId is null)
        )
        and t.name like :nameQuery
    """)
    fun searchWithTags(nameQuery: String, tags: List<TagId>, includeUntagged: Boolean): Flow<List<Track>>

    fun searchFolder(folderId: FolderId?, nameQuery: String, tags: List<TagId>, includeUntagged: Boolean): Flow<List<Track>> {
        return if (folderId == null) {
            if (tags.isEmpty()) {
                searchRootFolder(nameQuery)
            } else {
                searchRootFolder(nameQuery, tags, includeUntagged)
            }
        } else {
            if (tags.isEmpty()) {
                searchFolder(folderId, nameQuery)
            } else {
                searchFolder(folderId, nameQuery, tags, includeUntagged)
            }
        }
    }

    @Query("select * from track where folderId is null and name like :nameQuery")
    fun searchRootFolder(nameQuery: String): Flow<List<Track>>

    @Query("""
        select distinct t.*
        from track t
        left join tagtrackcrossref cr on cr.trackId = t.id
        where t.folderId is null
        and t.name like :nameQuery
        and (
            cr.tagId in (:tags)
            or (:includeUntagged and cr.trackId is null)
        )
    """)
    fun searchRootFolder(nameQuery: String, tags: List<TagId>, includeUntagged: Boolean): Flow<List<Track>>

    @Query("select * from track where folderId = :folderId and name like :nameQuery")
    fun searchFolder(folderId: FolderId, nameQuery: String): Flow<List<Track>>

    @Query("""
        select distinct t.*
        from track t
        left join tagtrackcrossref cr on cr.trackId = t.id
        where t.folderId = :folderId
        and t.name like :nameQuery
        and (
            cr.tagId in (:tags)
            or (:includeUntagged and cr.trackId is null)
        )
    """)
    fun searchFolder(folderId: FolderId, nameQuery: String, tags: List<TagId>, includeUntagged: Boolean): Flow<List<Track>>

    @Query(
        """
        select t.*
        from track t
        join artisttrackcrossref cr on cr.trackId = t.id
        where cr.artistId = :artistId
        and t.name like :nameQuery
    """
    )
    fun searchArtist(artistId: ArtistId, nameQuery: String): Flow<List<Track>>

    @Query(
        """
        select t.*
        from track t
        join playlisttrackcrossref cr on cr.trackId = t.id
        where cr.playlistId = :playlistId
        and t.name like :nameQuery
    """
    )
    fun searchPlaylist(playlistId: PlaylistId, nameQuery: String): Flow<List<Track>>

    @Query("""
        select t.*
        from track t
        join tagtrackcrossref cr on cr.trackId = t.id
        where cr.tagId = :tag
        and t.name like :nameQuery
    """)
    fun searchTag(nameQuery: String, tag: TagId): Flow<List<Track>>

    @Query("select * from track where folderId = :folderId")
    fun getFolderTracks(folderId: FolderId): Flow<List<Track>>

    @Query("select * from track where folderId is null")
    fun getRootFolderTracks(): Flow<List<Track>>

    @Query(
        """
        select t.*
        from track t
        join artisttrackcrossref cr on cr.trackId = t.id
        where cr.artistId = :artistId
    """
    )
    fun getArtistTracks(artistId: ArtistId): Flow<List<Track>>

    @Query(
        """
        select t.*
        from track t
        join playlisttrackcrossref cr on cr.trackId = t.id
        where cr.playlistId = :playlistId
    """
    )
    fun getPlaylistTracks(playlistId: PlaylistId): Flow<List<Track>>

    @Query("""
        select t.*
        from track t
        join mediafiletrackcrossref mftcr on mftcr.trackId = t.id
        join mediafileimportsessionitemcrossref mfisicr on mfisicr.mediaFileId = mftcr.mediaFileId
        where mfisicr.importSessionItemId = :id
    """)
    fun getImportSessionTrack(id: ImportSessionItemId): Flow<Track?>

    @Query(
        """
        insert into track (name, folderId, album, lyrics, albumTrackNumber, durationMillis, creationDatetime, updateDatetime)
        values (:name, :folderId, :album, :lyrics, :albumTrackNumber, :durationMillis, :creationDatetime, :updateDatetime)
    """
    )
    suspend fun add(
        name: String,
        folderId: FolderId?,
        album: String?,
        lyrics: String?,
        albumTrackNumber: Int?,
        durationMillis: Long,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long

    @Query("update track set name = :name, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateName(name: String, updateDatetime: Long, id: TrackId)

    @Query("update track set folderId = :folderId, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateFolderId(folderId: FolderId, updateDatetime: Long, id: TrackId)

    @Query("delete from track where id = :id")
    suspend fun delete(id: TrackId)
}