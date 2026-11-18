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
        val target = when (playlist) {
            is PlaylistToAddTo.Id -> playlist.value
            is PlaylistToAddTo.New -> PlaylistId(addPlaylist(playlist.name))
        }
        lockFor(target).withLock {
            addTracks(tracks, target)
            addPlaylists(playlists, target)
            addFolders(folders, target)
            addArtists(artists, target)
            addAlbums(albums, target)
        }
    }

    protected suspend fun addTracks(tracks: List<TrackId>, target: PlaylistId) {
        var position = (getMaxPosition(target) ?: -1) + 1
        for (track in tracks) {
            val inserted = addTrackToPlaylistIfNotExist(
                playlistId = target,
                trackId = track,
                position = position
            ) != -1L
            if (inserted) {
                position++
            }
        }
    }

    protected suspend fun addPlaylists(playlists: List<PlaylistId>, target: PlaylistId, ) {
        for (source in playlists) {
            val tracks = getPlaylistTracks(source).map { TrackId(it) }
            addTracks(tracks, target)
        }
    }

    protected suspend fun addFolders(folders: List<FolderId>, target: PlaylistId) {
        for (folder in folders) {
            addTracks(getFolderTracksRecursive(folder), target)
        }
    }

    protected suspend fun addArtists(artists: List<SpotifyArtistId>, target: PlaylistId) {
        for (artist in artists) {
            val tracks = getArtistTracks(artist).map { TrackId(it) }
            addTracks(tracks, target)
        }
    }

    protected suspend fun addAlbums(albums: List<SpotifyAlbumId>, target: PlaylistId) {
        for (album in albums) {
            val tracks = getAlbumTracks(album).map { TrackId(it) }
            addTracks(tracks, target)
        }
    }

    protected suspend fun getFolderTracksRecursive(folderId: FolderId): List<TrackId> {
        val tracks: List<TrackId> = getFolderTracks(folderId).map { TrackId(it) }
        val playlistTracks = getFolderPlaylists(folderId).flatMap {
            getPlaylistTracks(PlaylistId(it)).map { TrackId(it) }
        }
        val subfolderTracks = getSubfolders(folderId).flatMap {
            getFolderTracksRecursive(FolderId(it))
        }
        return tracks + playlistTracks + subfolderTracks
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
        select :playlistId, :trackId, :position, :creationDatetime, :updateDatetime
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
        position: Int,
        creationDatetime: Long = System.currentTimeMillis(),
        updateDatetime: Long = System.currentTimeMillis()
    ): Long

    @Query("select max(position) from playlisttrackcrossref where playlistId = :playlistId")
    abstract suspend fun getMaxPosition(playlistId: PlaylistId): Int?

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