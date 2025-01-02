package dev.younesgouyd.apps.music.app.data.repoes

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import dev.younesgouyd.apps.music.app.data.sqldelight.migrations.Playlist
import dev.younesgouyd.apps.music.app.data.sqldelight.queries.PlaylistQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant

class PlaylistRepo(private val queries: PlaylistQueries) {
    fun getAll(): Flow<List<Playlist>> {
        return queries.getAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun get(id: Long): Flow<Playlist> {
        return queries.get(id)
            .asFlow()
            .mapToOne(Dispatchers.IO)
    }

    suspend fun getStatic(id: Long): Playlist? {
        return withContext(Dispatchers.IO) {
            queries.get(id).executeAsOneOrNull()
        }
    }

    suspend fun add(name: String, folderId: Long?, image: ByteArray?): Long {
        require(name.isNotEmpty())
        return withContext(Dispatchers.IO) {
            val currentTime = Instant.now().toEpochMilli()
            queries.add(
                name = name,
                folder_id = folderId,
                image = image,
                creation_datetime = currentTime,
                update_datetime = currentTime
            ).executeAsOne()
        }
    }

    suspend fun updateName(id: Long, name: String) {
        require(name.isNotEmpty())
        withContext(Dispatchers.IO) {
            queries.updateName(name, Instant.now().toEpochMilli(), id)
        }
    }

    suspend fun updateFolderId(id: Long, folderId: Long) {
        withContext(Dispatchers.IO) {
            queries.updateFolderId(folderId, Instant.now().toEpochMilli(), id)
        }
    }

    suspend fun updateImage(id: Long, image: ByteArray?) {
        withContext(Dispatchers.IO) {
            queries.updateImage(image, Instant.now().toEpochMilli(), id)
        }
    }

    suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) {
            queries.delete(id)
        }
    }

    fun getFolderPlaylists(folderId: Long?): Flow<List<Playlist>> {
        return queries.getFolderPlaylists(folderId)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    suspend fun getFolderPlaylistsStatic(folderId: Long?): List<Playlist> {
        return withContext(Dispatchers.IO) {
            queries.getFolderPlaylists(folderId).executeAsList()
        }
    }

    fun getTrackPlaylists(trackId: Long): Flow<List<Playlist>> {
        return queries.getTrackPlaylists(trackId)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }
}