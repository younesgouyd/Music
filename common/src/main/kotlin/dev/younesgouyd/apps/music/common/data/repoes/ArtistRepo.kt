package dev.younesgouyd.apps.music.common.data.repoes

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import dev.younesgouyd.apps.music.common.data.sqldelight.migrations.Artist
import dev.younesgouyd.apps.music.common.data.sqldelight.queries.ArtistQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ArtistRepo(private val queries: ArtistQueries) {
    fun getAll(): Flow<List<Artist>> {
        return queries.getAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun get(id: Long): Flow<Artist> {
        return queries.get(id)
            .asFlow()
            .mapToOne(Dispatchers.IO)
    }

    suspend fun add(name: String, image: ByteArray?): Long {
        require(name.isNotEmpty())
        return withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            queries.add(
                name = name,
                image = image,
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

    suspend fun updateImage(id: Long, image: ByteArray?) {
        withContext(Dispatchers.IO) {
            queries.updateImage(image, System.currentTimeMillis(), id)
        }
    }

    suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) {
            queries.delete(id)
        }
    }

    fun getTrackArtists(trackId: Long): Flow<List<Artist>> {
        return queries.getTrackArtists(trackId)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    suspend fun getTrackArtistsStatic(trackId: Long): List<Artist> {
        return withContext(Dispatchers.IO) {
            queries.getTrackArtists(trackId).executeAsList()
        }
    }

    suspend fun getAlbumArtistsStatic(albumId: Long): List<Artist> {
        return withContext(Dispatchers.IO) {
            queries.getAlbumArtists(albumId).executeAsList()
        }
    }

    suspend fun getByName(name: String): List<Artist> {
        return withContext(Dispatchers.IO) {
            queries.getByName(name).executeAsList()
        }
    }
}