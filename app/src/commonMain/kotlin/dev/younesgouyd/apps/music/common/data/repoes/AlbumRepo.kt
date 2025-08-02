package dev.younesgouyd.apps.music.common.data.repoes

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import dev.younesgouyd.apps.music.common.data.sqldelight.migrations.Album
import dev.younesgouyd.apps.music.common.data.sqldelight.queries.AlbumQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AlbumRepo(private val queries: AlbumQueries) {
    fun getAll(): Flow<List<Album>> {
        return queries.getAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun get(id: Long): Flow<Album> {
        return queries.get(id)
            .asFlow()
            .mapToOne(Dispatchers.IO)
    }

    suspend fun getStatic(id: Long): Album? {
        return withContext(Dispatchers.IO) {
            queries.get(id).executeAsOneOrNull()
        }
    }

    suspend fun add(name: String, image: ByteArray?, releaseDate: String?): Long {
        require(name.isNotEmpty())
        return withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            queries.add(
                name = name,
                image = image,
                release_date = releaseDate,
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

    suspend fun updateReleaseDate(id: Long, releaseDate: String?) {
        withContext(Dispatchers.IO) {
            queries.updateReleaseDate(releaseDate, System.currentTimeMillis(), id)
        }
    }

    suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) {
            queries.delete(id)
        }
    }

    fun getArtistAlbums(artistId: Long): Flow<List<Album>> {
        return queries.getArtistAlbums(artistId)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    suspend fun getByName(name: String): List<Album> {
        return withContext(Dispatchers.IO) {
            queries.getByName(name).executeAsList()
        }
    }
}