package dev.younesgouyd.apps.music.common.data.repoes

import dev.younesgouyd.apps.music.common.Base64String
import dev.younesgouyd.apps.music.common.data.room.entities.Track
import dev.younesgouyd.apps.music.common.data.room.entities.TrackDao
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

class TrackRepo(private val dao: TrackDao) {
    fun getAll(): Flow<List<Track>> {
        return dao.getAll()
    }

    fun get(id: Long): Flow<Track> {
        return dao.get(id)
    }

    fun getArtistTracks(artistId: Long): Flow<List<Track>> {
        return dao.getArtistTracks(artistId)
    }

    fun getFolderTracks(folderId: Long?): Flow<List<Track>> {
        return if (folderId != null) dao.getFolderTracks(folderId) else dao.getRootFolderTracks()
    }

    fun getPlaylistTracks(playlistId: Long): Flow<List<Track>> {
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