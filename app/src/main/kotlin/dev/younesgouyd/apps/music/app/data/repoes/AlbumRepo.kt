package dev.younesgouyd.apps.music.app.data.repoes

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import dev.younesgouyd.apps.music.app.data.sqldelight.migrations.Album
import dev.younesgouyd.apps.music.app.data.sqldelight.queries.AlbumQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant

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

    suspend fun getStatic(id: Long): Album {
        return withContext(Dispatchers.IO) {
            queries.get(id).executeAsOne()
        }
    }

    suspend fun add(name: String, image: ByteArray?, releaseDate: String?): Long {
        require(name.isNotEmpty())
        return withContext(Dispatchers.IO) {
            val currentTime = Instant.now().toEpochMilli()
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
            queries.updateName(name, Instant.now().toEpochMilli(), id)
        }
    }

    suspend fun updateImage(id: Long, image: ByteArray?) {
        withContext(Dispatchers.IO) {
            queries.updateImage(image, Instant.now().toEpochMilli(), id)
        }
    }

    suspend fun updateReleaseDate(id: Long, releaseDate: String?) {
        withContext(Dispatchers.IO) {
            queries.updateReleaseDate(releaseDate, Instant.now().toEpochMilli(), id)
        }
    }

    suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) {
            queries.delete(id)
        }
    }
}