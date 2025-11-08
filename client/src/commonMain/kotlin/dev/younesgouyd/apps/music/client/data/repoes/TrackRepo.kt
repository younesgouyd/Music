package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.room.toSearchQuery
import dev.younesgouyd.apps.music.common.Base64String
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

class TrackRepo(private val dao: dev.younesgouyd.apps.music.client.data.room.entities.TrackDao) {
    fun get(id: Long): Flow<dev.younesgouyd.apps.music.client.data.room.entities.Track> {
        return dao.get(id)
    }

    fun searchFolder(
        folderId: Long?,
        nameQuery: String
    ): Flow<List<dev.younesgouyd.apps.music.client.data.room.entities.Track>> {
        return dao.searchFolder(folderId, nameQuery.toSearchQuery())
    }

    fun searchArtist(
        artistId: Long,
        nameQuery: String
    ): Flow<List<dev.younesgouyd.apps.music.client.data.room.entities.Track>> {
        return dao.searchArtist(artistId, nameQuery.toSearchQuery())
    }

    fun searchPlaylist(
        playlistId: Long,
        nameQuery: String
    ): Flow<List<dev.younesgouyd.apps.music.client.data.room.entities.Track>> {
        return dao.searchPlaylist(playlistId, nameQuery.toSearchQuery())
    }

    fun getFolderTracks(folderId: Long?): Flow<List<dev.younesgouyd.apps.music.client.data.room.entities.Track>> {
        return if (folderId != null) dao.getFolderTracks(folderId) else dao.getRootFolderTracks()
    }

    fun getPlaylistTracks(playlistId: Long): Flow<List<dev.younesgouyd.apps.music.client.data.room.entities.Track>> {
        return dao.getPlaylistTracks(playlistId)
    }

    suspend fun add(
        name: String,
        folderId: Long?,
        album: String?,
        albumArt: Base64String?,
        lyrics: String?,
        albumTrackNumber: Int?,
        duration: Duration
    ): Long {
        require(name.isNotEmpty())
        val currentTime = System.currentTimeMillis()
        return dao.add(
            name = name,
            folderId = folderId,
            album = album,
            albumArt = albumArt,
            lyrics = lyrics,
            albumTrackNumber = albumTrackNumber,
            durationMillis = duration.inWholeMilliseconds,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }

    suspend fun updateName(id: Long, name: String) {
        require(name.isNotEmpty())
        dao.updateName(name, System.currentTimeMillis(), id)
    }

    suspend fun updateFolderId(id: Long, folderId: Long) {
        dao.updateFolderId(folderId, System.currentTimeMillis(), id)
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
    }
}