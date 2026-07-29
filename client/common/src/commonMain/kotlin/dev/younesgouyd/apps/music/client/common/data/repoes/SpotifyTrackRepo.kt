package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.common.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.SpotifyTrackId
import dev.younesgouyd.apps.music.common.SpotifyTrackRelation
import io.ktor.client.*
import kotlinx.coroutines.flow.Flow

class SpotifyTrackRepo(
    private val client: HttpClient
) {
    suspend fun getId(spotifyId: String): SpotifyTrackId? {
        TODO()
//        return dao.getId(spotifyId)?.let {
//            SpotifyTrackId(it)
//        }
    }

    fun getAlbumTracks(id: SpotifyAlbumId): Flow<List<SpotifyTrackRelation>> {
        TODO()
//        return dao.getAlbumTracks(id)
    }
}