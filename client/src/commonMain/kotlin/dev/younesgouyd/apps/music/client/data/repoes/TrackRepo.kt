package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.ArtistId
import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.PlaylistId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.room.entities.Track
import dev.younesgouyd.apps.music.client.data.room.entities.TrackDao
import dev.younesgouyd.apps.music.client.data.room.toSearchQuery
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

class TrackRepo(
    private val dao: TrackDao
) {
    fun getAll(): Flow<List<Track>> {
        return dao.getAll()
    }

    fun get(id: TrackId): Flow<Track> {
        return dao.get(id)
    }

    fun searchFolder(
        folderId: FolderId?,
        nameQuery: String
    ): Flow<List<Track>> {
        return dao.searchFolder(folderId, nameQuery.toSearchQuery())
    }

    fun searchArtist(
        artistId: ArtistId,
        nameQuery: String
    ): Flow<List<Track>> {
        return dao.searchArtist(artistId, nameQuery.toSearchQuery())
    }

    fun searchPlaylist(
        playlistId: PlaylistId,
        nameQuery: String
    ): Flow<List<Track>> {
        return dao.searchPlaylist(playlistId, nameQuery.toSearchQuery())
    }

    fun getFolderTracks(folderId: FolderId?): Flow<List<Track>> {
        return if (folderId != null) dao.getFolderTracks(folderId) else dao.getRootFolderTracks()
    }

    fun getArtistTracks(artistId: ArtistId): Flow<List<Track>> {
        return dao.getArtistTracks(artistId)
    }

    fun getPlaylistTracks(playlistId: PlaylistId): Flow<List<Track>> {
        return dao.getPlaylistTracks(playlistId)
    }

    suspend fun add(
        name: String,
        folderId: FolderId?,
        album: String?,
        lyrics: String?,
        albumTrackNumber: Int?,
        duration: Duration
    ): TrackId {
        require(name.isNotEmpty())
        val currentTime = System.currentTimeMillis()
        val id = dao.add(
            name = name,
            folderId = folderId,
            album = album,
            lyrics = lyrics,
            albumTrackNumber = albumTrackNumber,
            durationMillis = duration.inWholeMilliseconds,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
        return TrackId(id)
    }

    suspend fun updateName(id: TrackId, name: String) {
        require(name.isNotEmpty())
        dao.updateName(name, System.currentTimeMillis(), id)
    }

    suspend fun updateFolderId(id: TrackId, folderId: FolderId) {
        dao.updateFolderId(folderId, System.currentTimeMillis(), id)
    }

    suspend fun delete(id: TrackId) {
        dao.delete(id)
    }
}