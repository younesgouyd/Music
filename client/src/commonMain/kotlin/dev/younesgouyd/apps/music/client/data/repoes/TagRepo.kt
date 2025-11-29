package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.TagId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.room.entities.Tag
import dev.younesgouyd.apps.music.client.data.room.entities.TagDao
import dev.younesgouyd.apps.music.client.data.room.toSearchQuery
import kotlinx.coroutines.flow.Flow

class TagRepo(
    private val dao: TagDao
) {
    fun getAll(): Flow<List<Tag>> {
        return dao.getAll()
    }

    fun search(nameQuery: String): Flow<List<Tag>> {
        return dao.search(nameQuery.toSearchQuery())
    }

    fun getTrackTags(trackId: TrackId): Flow<List<Tag>> {
        return dao.getTrackTags(trackId)
    }

    suspend fun add(name: String, description: String?) {
        require(name.isNotBlank())
        val currentTime = System.currentTimeMillis()
        dao.add(
            name = name,
            description = description,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
    }

    suspend fun updateDescription(id: TagId, description: String?) {
        dao.updateDescription(
            description = description,
            updateDatetime = System.currentTimeMillis(),
            id = id
        )
    }

    suspend fun delete(id: TagId) {
        dao.delete(id)
    }
}