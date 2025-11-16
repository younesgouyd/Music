package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.*
import kotlinx.coroutines.flow.Flow
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
data class Track(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val folderId: Long?,
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
    @Query("select * from track where id = :id")
    fun get(id: Long): Flow<Track>

    fun searchFolder(folderId: Long?, nameQuery: String): Flow<List<Track>> {
        val _nameQ = nameQuery.ifEmpty { "%" }
        return if (folderId == null) {
            searchRootFolder(_nameQ)
        } else {
            searchFolder(folderId, _nameQ)
        }
    }

    @Query("select * from track where folderId is null and name like :nameQuery")
    fun searchRootFolder(nameQuery: String): Flow<List<Track>>

    @Query("select * from track where folderId = :folderId and name like :nameQuery")
    fun searchFolder(folderId: Long, nameQuery: String): Flow<List<Track>>

    @Query(
        """
        select t.*
        from track t
        join artisttrackcrossref cr on cr.trackId = t.id
        where cr.artistId = :artistId
        and t.name like :nameQuery
    """
    )
    fun searchArtist(artistId: Long, nameQuery: String): Flow<List<Track>>

    @Query(
        """
        select t.*
        from track t
        join playlisttrackcrossref cr on cr.trackId = t.id
        where cr.playlistId = :playlistId
        and name like :nameQuery
    """
    )
    fun searchPlaylist(playlistId: Long, nameQuery: String): Flow<List<Track>>

    @Query("select * from track where folderId = :folderId")
    fun getFolderTracks(folderId: Long): Flow<List<Track>>

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
    fun getArtistTracks(artistId: Long): Flow<List<Track>>

    @Query(
        """
        select t.*
        from track t
        join playlisttrackcrossref cr on cr.trackId = t.id
        where cr.playlistId = :playlistId
    """
    )
    fun getPlaylistTracks(playlistId: Long): Flow<List<Track>>

    @Query(
        """
        insert into track (name, folderId, album, lyrics, albumTrackNumber, durationMillis, creationDatetime, updateDatetime)
        values (:name, :folderId, :album, :lyrics, :albumTrackNumber, :durationMillis, :creationDatetime, :updateDatetime)
    """
    )
    suspend fun add(
        name: String,
        folderId: Long?,
        album: String?,
        lyrics: String?,
        albumTrackNumber: Int?,
        durationMillis: Long,
        creationDatetime: Long,
        updateDatetime: Long
    ): Long

    @Query("update track set name = :name, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateName(name: String, updateDatetime: Long, id: Long)

    @Query("update track set folderId = :folderId, updateDatetime = :updateDatetime where id = :id")
    suspend fun updateFolderId(folderId: Long, updateDatetime: Long, id: Long)

    @Query("delete from track where id = :id")
    suspend fun delete(id: Long)
}