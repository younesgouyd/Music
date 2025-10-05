package dev.younesgouyd.apps.music.common.data.repoes

import dev.younesgouyd.apps.music.common.data.room.entities.Track
import dev.younesgouyd.apps.music.common.data.room.entities.TrackDao
import kotlinx.coroutines.flow.Flow

class TrackRepo(private val dao: TrackDao) {
    fun getAll(): Flow<List<Track>> {
        return dao.getAll()
    }

    fun get(id: Long): Flow<Track> {
        return dao.get(id)
    }

    fun getAlbumTracks(albumId: Long): Flow<List<Track>> {
        return dao.getAlbumTracks(albumId)
    }

    fun getArtistTracks(artistId: Long): Flow<List<Track>> {
        return dao.getArtistTracks(artistId)
    }

    fun getFolderTracks(folderId: Long): Flow<List<Track>> {
        return dao.getFolderTracks(folderId)
    }

    fun getPlaylistTracks(playlistId: Long): Flow<List<Track>> {
        return dao.getPlaylistTracks(playlistId)
    }

    suspend fun add(name: String, folderId: Long, albumId: Long?, lyrics: String?, albumTrackNumber: Int?, durationMillis: Long?): Long {
        require(name.isNotEmpty())
        val currentTime = System.currentTimeMillis()
        return dao.add(
            name = name,
            folderId = folderId,
            albumId = albumId,
            lyrics = lyrics,
            albumTrackNumber = albumTrackNumber,
            durationMillis = durationMillis,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }

    suspend fun updateName(id: Long, name: String) {
        require(name.isNotEmpty())
        dao.updateName(name, System.currentTimeMillis(), id)
    }

    suspend fun updateAlbumId(id: Long, albumId: Long?) {
        dao.updateAlbumId(albumId, System.currentTimeMillis(), id)
    }

    suspend fun updateFolderId(id: Long, folderId: Long) {
        dao.updateFolderId(folderId, System.currentTimeMillis(), id)
    }

    suspend fun updateLyrics(id: Long, lyrics: String?) {
        dao.updateLyrics(lyrics, System.currentTimeMillis(), id)
    }

    suspend fun updateAlbumTrackNumber(id: Long, albumTrackNumber: Int?) {
        dao.updateAlbumTrackNumber(albumTrackNumber, System.currentTimeMillis(), id)
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
    }
}