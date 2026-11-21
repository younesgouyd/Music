package dev.younesgouyd.apps.music.server.multiplatform.data.room.transactions

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.PlaylistTrackCrossRef

@Dao
abstract class AddToPlaylist {
    @Transaction
    open suspend fun execute(
        tracks: Set<TrackId>,
        playlists: Set<PlaylistId>,
        folders: Set<FolderId>,
        artists: Set<SpotifyArtistId>,
        albums: Set<SpotifyAlbumId>,
        playlist: PlaylistToAddTo
    ) {
        val isNew = playlist is PlaylistToAddTo.New
        val target = when (playlist) {
            is PlaylistToAddTo.Id -> playlist.value
            is PlaylistToAddTo.New -> PlaylistId(addPlaylist(playlist.name))
        }
        val existing: Set<Long> = if (isNew) emptySet() else getPlaylistTracks(target).toSet()
        val toBeAdded = mutableSetOf<Long>()
        toBeAdded.addAll(tracks.map { it.value } - existing)
        for (playlist in playlists) { toBeAdded.addAll(getPlaylistTracks(playlist) - existing) }
        for (folder in folders) { toBeAdded.addAll(getFolderTracksRecursive(folder) - existing) }
        for (artist in artists) { toBeAdded.addAll(getArtistTracks(artist) - existing) }
        for (album in albums) { toBeAdded.addAll(getAlbumTracks(album) - existing) }
        if (toBeAdded.isEmpty()) return
        var position = if (isNew) 0 else (getMaxPosition(target) ?: -1) + 1
        val currentTime = System.currentTimeMillis()
        addAll(
            toBeAdded.map {
                PlaylistTrackCrossRef(
                    playlistId = target,
                    trackId = TrackId(it),
                    position = position++,
                    creationDatetime = currentTime,
                    updateDatetime = currentTime
                )
            }
        )
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

    @Insert
    abstract suspend fun addAll(crossRefs: List<PlaylistTrackCrossRef>)

    @Query("select max(position) from playlisttrackcrossref where playlistId = :playlistId")
    abstract suspend fun getMaxPosition(playlistId: PlaylistId): Int?

    @Query("""
        with recursive folders(id) as (
            select id from folder where id = :folderId
            union
            select f.id from folder f join folders fs on f.parentFolderId = fs.id
        )
        select id from track where folderId in (select id from folders)
        union
        select cr.trackId
        from playlist p
        join playlisttrackcrossref cr on cr.playlistId = p.id
        where p.folderId in (select id from folders)
    """)
    protected abstract suspend fun getFolderTracksRecursive(folderId: FolderId): List<Long>

    @Query("select trackId from playlisttrackcrossref where playlistId = :id order by position asc")
    abstract suspend fun getPlaylistTracks(id: PlaylistId): List<Long>

    @Query("""
        select t.id
        from track t
        join spotifyartistspotifytrackcrossref cr on cr.spotifyTrackId = t.spotifyTrackId
        where cr.spotifyArtistId = :id
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