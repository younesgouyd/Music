package dev.younesgouyd.apps.music.server.multiplatform.data.room.transactions

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.server.multiplatform.data.room.daos.PlaylistTrackCrossRefDao.Companion.lockFor
import kotlinx.coroutines.sync.withLock

@Dao
abstract class AddToPlaylist {
    @Transaction
    open suspend fun execute(
        tracks: List<TrackId>,
        playlists: List<PlaylistId>,
        folders: List<FolderId>,
        artists: List<SpotifyArtistId>,
        albums: List<SpotifyAlbumId>,
        playlist: PlaylistToAddTo
    ) {
        val playlistId = when (playlist) {
            is PlaylistToAddTo.Id -> playlist.value
            is PlaylistToAddTo.New -> PlaylistId(addPlaylist(playlist.name))
        }
        lockFor(playlistId).withLock {
            addTracks(playlistId, tracks)
            addPlaylists(playlistId, playlists)
            addFolders(playlistId, folders)
            addArtists(playlistId, artists)
            addAlbums(playlistId, albums)
        }
    }

    protected suspend fun addTracks(playlistId: PlaylistId, tracks: List<TrackId>) {
        for (track in tracks) {
            addTrackToPlaylistIfNotExist(
                playlistId = playlistId,
                trackId = track
            )
        }
    }

    protected suspend fun addPlaylists(playlistId: PlaylistId, playlists: List<PlaylistId>) {
        for (playlist in playlists) {
            val tracks = getPlaylistTracks(playlistId).map { TrackId(it) }
            addTracks(playlist, tracks)
        }
    }

    protected suspend fun addFolders(playlistId: PlaylistId, folders: List<FolderId>) {
        suspend fun _getFolderTracks(folderId: FolderId): List<TrackId> {
            val tracks: List<TrackId> = getFolderTracks(folderId).map { TrackId(it) }
            val playlistTracks = getFolderPlaylists(folderId).flatMap {
                getPlaylistTracks(PlaylistId(it)).map { TrackId(it) }
            }
            val subfolderTracks = getSubfolders(folderId).flatMap {
                _getFolderTracks(FolderId(it))
            }
            return tracks + playlistTracks + subfolderTracks
        }
        for (folder in folders) {
            addTracks(playlistId, _getFolderTracks(folder))
        }
    }

    protected suspend fun addArtists(playlistId: PlaylistId, artists: List<SpotifyArtistId>) {
        for (artist in artists) {
            val tracks = getArtistTracks(artist).map { TrackId(it) }
            addTracks(playlistId, tracks)
        }
    }

    protected suspend fun addAlbums(playlistId: PlaylistId, albums: List<SpotifyAlbumId>) {
        for (album in albums) {
            val tracks = getAlbumTracks(album).map { TrackId(it) }
            addTracks(playlistId, tracks)
        }
    }

    @Query(
        """
        insert into playlist (name, folderId, creationDatetime, updateDatetime)
        values (:name, NULL, :creationDatetime, :updateDatetime)
    """
    )
    abstract suspend fun addPlaylist(
        name: String,
        creationDatetime: Long = System.currentTimeMillis(),
        updateDatetime: Long = System.currentTimeMillis()
    ): Long

    @Query(
        """
        insert into playlisttrackcrossref (playlistId, trackId, position, creationDatetime, updateDatetime)
        select :playlistId,
            :trackId,
            coalesce((select max(position) + 1 from playlisttrackcrossref where playlistId = :playlistId), 0),
            :creationDatetime,
            :updateDatetime
        where not exists(
            select 1
            from playlisttrackcrossref
            where playlistId = :playlistId
            and trackId = :trackId
        )
    """
    )
    abstract suspend fun addTrackToPlaylistIfNotExist(
        playlistId: PlaylistId,
        trackId: TrackId,
        creationDatetime: Long = System.currentTimeMillis(),
        updateDatetime: Long = System.currentTimeMillis()
    )

//    @Query(
//        """
//        insert into playlisttrackcrossref (playlistId, trackId, position, creationDatetime, updateDatetime)
//        values (:playlistId, :trackId, :position, :creationDatetime, :updateDatetime)
//    """
//    )
//    abstract suspend fun addTrackToPlaylist(
//        playlistId: PlaylistId,
//        trackId: TrackId,
//        position: Int,
//        creationDatetime: Long = System.currentTimeMillis(),
//        updateDatetime: Long = System.currentTimeMillis()
//    )

//    @Query(
//        """
//        select not exists(
//            select 1
//            from playlisttrackcrossref
//            where playlistId = :playlistId
//            and trackId = :trackId
//        )
//    """
//    )
//    abstract suspend fun trackNotExists(playlistId: PlaylistId, trackId: TrackId): Boolean

//    @Query("""
//        select COALESCE(MAX(position), -1) + 1
//        from playlisttrackcrossref
//        where playlistId = :playlistId
//    """)
//    abstract suspend fun nextPosition(playlistId: PlaylistId): Int

    @Query("""
        select t.id
        from track t
        join playlisttrackcrossref cr on cr.trackId = t.id
        where cr.playlistId = :id
        order by cr.position
    """)
    abstract suspend fun getPlaylistTracks(id: PlaylistId): List<Long>

    @Query("select id from track where folderId = :id")
    abstract suspend fun getFolderTracks(id: FolderId): List<Long>

    @Query("select id from playlist where folderId = :folderId")
    abstract suspend fun getFolderPlaylists(folderId: FolderId): List<Long>

    @Query("select id from folder where parentFolderId = :parentFolderId order by name asc")
    abstract suspend fun getSubfolders(parentFolderId: FolderId): List<Long>

    @Query("""
        select t.id
        from track t
        join spotifyartistspotifytrackcrossref cr on cr.spotifyTrackId = t.spotifyTrackId
        and cr.spotifyArtistId = :id
    """)
    abstract suspend fun getArtistTracks(id: SpotifyArtistId): List<Long>

    @Query("""
        select t.id
        from track t
        join spotifytrack sp on sp.id = t.spotifyTrackId
        where sp.spotifyAlbumId = :id
        order by sp.trackNumber
    """)
    abstract suspend fun getAlbumTracks(id: SpotifyAlbumId): List<Long>
}