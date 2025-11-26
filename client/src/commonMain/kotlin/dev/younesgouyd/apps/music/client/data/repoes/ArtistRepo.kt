package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.ArtistId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.room.entities.Artist
import dev.younesgouyd.apps.music.client.data.room.entities.ArtistDao
import dev.younesgouyd.apps.music.client.data.room.toSearchQuery
import kotlinx.coroutines.flow.Flow

class ArtistRepo(private val dao: ArtistDao) {
    fun getAll(): Flow<List<Artist>> {
        return dao.getAll()
    }

    fun search(nameQuery: String): Flow<List<Artist>> {
        return dao.search(nameQuery.toSearchQuery())
    }

    fun get(id: ArtistId): Flow<Artist> {
        return dao.get(id)
    }

    fun getTrackArtists(trackId: TrackId): Flow<List<Artist>> {
        return dao.getTrackArtists(trackId)
    }

    fun getByName(name: String): Flow<List<Artist>> {
        return dao.getByName(name)
    }

    suspend fun add(name: String): ArtistId {
        require(name.isNotEmpty())
        val currentTime = System.currentTimeMillis()
        val id = dao.add(
            name = name,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
        return ArtistId(id)
    }

    suspend fun updateName(id: ArtistId, name: String) {
        require(name.isNotEmpty())
        dao.updateName(name, System.currentTimeMillis(), id)
    }
}