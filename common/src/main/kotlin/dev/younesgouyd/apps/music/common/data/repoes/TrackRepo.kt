package dev.younesgouyd.apps.music.common.data.repoes

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import dev.younesgouyd.apps.music.common.data.sqldelight.migrations.Track
import dev.younesgouyd.apps.music.common.data.sqldelight.queries.GetPlaylistTracks
import dev.younesgouyd.apps.music.common.data.sqldelight.queries.TrackQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TrackRepo(private val queries: TrackQueries) {
    fun getAll(): Flow<List<Track>> {
        return queries.getAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun get(id: Long): Flow<Track> {
        return queries.get(id)
            .asFlow()
            .mapToOne(Dispatchers.IO)
    }

    suspend fun getStatic(id: Long): Track? {
        return withContext(Dispatchers.IO) {
            queries.get(id).executeAsOneOrNull()
        }
    }

    suspend fun add(name: String, folderId: Long, albumId: Long?, audioUrl: String?, videoUrl: String?, lyrics: String?, albumTrackNumber: Long?, duration: Long): Long {
        require(name.isNotEmpty())
        return withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            queries.add(
                name = name,
                folder_id = folderId,
                album_id = albumId,
                audio_url = audioUrl,
                video_url = videoUrl,
                lyrics = lyrics,
                album_track_number = albumTrackNumber,
                duration = duration,
                creation_datetime = currentTime,
                update_datetime = currentTime
            ).executeAsOne()
        }
    }

    suspend fun updateName(id: Long, name: String) {
        require(name.isNotEmpty())
        withContext(Dispatchers.IO) {
            queries.updateName(name, System.currentTimeMillis(), id)
        }
    }

    suspend fun updateAlbumId(id: Long, albumId: Long?) {
        withContext(Dispatchers.IO) {
            queries.updateAlbumId(albumId, System.currentTimeMillis(), id)
        }
    }

    suspend fun updateFolderId(id: Long, folderId: Long) {
        withContext(Dispatchers.IO) {
            queries.updateFolderId(folderId, System.currentTimeMillis(), id)
        }
    }

    suspend fun updateAudioUrl(id: Long, audioUrl: String?) {
        withContext(Dispatchers.IO) {
            queries.updateAudioUrl(audioUrl, System.currentTimeMillis(), id)
        }
    }

    suspend fun updateVideoUrl(id: Long, videoUrl: String?) {
        withContext(Dispatchers.IO) {
            queries.updateVideoUrl(videoUrl, System.currentTimeMillis(), id)
        }
    }

    suspend fun updateLyrics(id: Long, lyrics: String?) {
        withContext(Dispatchers.IO) {
            queries.updateLyrics(lyrics, System.currentTimeMillis(), id)
        }
    }

    suspend fun updateAlbumTrackNumber(id: Long, albumTrackNumber: Long?) {
        withContext(Dispatchers.IO) {
            queries.updateAlbumTrackNumber(albumTrackNumber, System.currentTimeMillis(), id)
        }
    }

    suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) {
            queries.delete(id)
        }
    }

    fun getAlbumTracks(albumId: Long): Flow<List<Track>> {
        return queries.getAlbumTracks(albumId)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    suspend fun getAlbumTracksStatic(albumId: Long): List<Track> {
        return withContext(Dispatchers.IO) {
            queries.getAlbumTracks(albumId).executeAsList()
        }
    }

    fun getArtistTracks(artistId: Long): Flow<List<Track>> {
        return queries.getArtistTracks(artistId)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    suspend fun getArtistTracksStatic(artistId: Long): List<Track> {
        return withContext(Dispatchers.IO) {
            queries.getArtistTracks(artistId).executeAsList()
        }
    }

    fun getFolderTracks(folderId: Long): Flow<List<Track>> {
        return queries.getFolderTracks(folderId)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    suspend fun getFolderTracksStatic(folderId: Long): List<Track> {
        return withContext(Dispatchers.IO) {
            queries.getFolderTracks(folderId).executeAsList()
        }
    }

    fun getPlaylistTracks(playlistId: Long): Flow<List<GetPlaylistTracks>> {
        return queries.getPlaylistTracks(playlistId)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    suspend fun getPlaylistTracksStatic(playlistId: Long): List<GetPlaylistTracks> {
        return withContext(Dispatchers.IO) {
            queries.getPlaylistTracks(playlistId).executeAsList()
        }
    }
}