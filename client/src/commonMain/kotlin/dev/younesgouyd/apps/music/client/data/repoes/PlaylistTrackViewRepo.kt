package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.PlaylistId
import dev.younesgouyd.apps.music.client.data.room.PlaylistTrackView
import dev.younesgouyd.apps.music.client.data.room.entities.PlaylistTrackViewDao
import dev.younesgouyd.apps.music.client.data.room.toSearchQuery
import kotlinx.coroutines.flow.Flow

class PlaylistTrackViewRepo(
    val dao: PlaylistTrackViewDao
) {
    fun search(id: PlaylistId, nameQuery: String): Flow<List<PlaylistTrackView>> {
        return dao.search(id, nameQuery.toSearchQuery())
    }
}