package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.room.toSearchQuery
import kotlinx.coroutines.flow.Flow

class ArtistRepo(private val dao: dev.younesgouyd.apps.music.client.data.room.entities.ArtistDao) {
    fun search(nameQuery: String): Flow<List<dev.younesgouyd.apps.music.client.data.room.entities.Artist>> {
        return dao.search(nameQuery.toSearchQuery())
    }

    fun get(id: Long): Flow<dev.younesgouyd.apps.music.client.data.room.entities.Artist> {
        return dao.get(id)
    }

    fun getTrackArtists(trackId: Long): Flow<List<dev.younesgouyd.apps.music.client.data.room.entities.Artist>> {
        return dao.getTrackArtists(trackId)
    }

    fun getByName(name: String): Flow<List<dev.younesgouyd.apps.music.client.data.room.entities.Artist>> {
        return dao.getByName(name)
    }

    suspend fun add(name: String, image: ByteArray?): Long {
        require(name.isNotEmpty())
        val currentTime = System.currentTimeMillis()
        return dao.add(
            name = name,
            image = image,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }

    suspend fun updateName(id: Long, name: String) {
        require(name.isNotEmpty())
        dao.updateName(name, System.currentTimeMillis(), id)
    }

    suspend fun updateImage(id: Long, image: ByteArray?) {
        dao.updateImage(image, System.currentTimeMillis(), id)
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
    }
}