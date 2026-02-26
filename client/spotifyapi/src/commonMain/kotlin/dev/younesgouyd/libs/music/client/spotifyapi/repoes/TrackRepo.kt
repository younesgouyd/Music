package dev.younesgouyd.libs.music.client.spotifyapi.repoes

import dev.younesgouyd.libs.music.client.spotifyapi.ApiRespJson
import dev.younesgouyd.libs.music.client.spotifyapi.models.AlbumTracks
import dev.younesgouyd.libs.music.client.spotifyapi.models.Track
import dev.younesgouyd.libs.music.client.spotifyapi.models.common.AlbumId
import dev.younesgouyd.libs.music.client.spotifyapi.models.common.TrackId
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

internal class TrackRepo(
    private val client: HttpClient,
    private val serializer: Json,
    private val getToken: suspend () -> String
) {

    suspend fun getAlbumTracks(id: AlbumId): List<Pair<ApiRespJson, Track>> {
        val trackIds = mutableSetOf<TrackId>()
        val response = getAlbumTracks(id, Index.initial())
        trackIds.addAll(response.items.map { it.id })
        var next = Index.fromUrl(response.next)
        while (next != null) {
            val data = getAlbumTracks(id, next)
            trackIds.addAll(data.items.map { it.id })
            next = Index.fromUrl(data.next)
        }
        return buildList {
            for (id in trackIds) {
                add(get(id))
            }
        }
    }

    /**
     * GET /albums/{id}/tracks
     * Get Spotify catalog information about an album’s tracks. Optional parameters can be used to limit the number
     * of tracks returned.
     */
    private suspend fun getAlbumTracks(id: AlbumId, offset: Index): AlbumTracks {
        val response = client.get("albums/$id/tracks") {
            header("Authorization", "Bearer ${getToken()}")
            parameter("limit", 50)
            parameter("offset", offset.value)
        }.bodyAsText()
        return serializer.decodeFromString<AlbumTracks>(response)
    }

    private suspend fun get(id: TrackId): Pair<ApiRespJson, Track> {
        val response = client.get("tracks/$id") {
            header("Authorization", "Bearer ${getToken()}")
        }.bodyAsText()
        return response to serializer.decodeFromString<Track>(response)
    }

    private data class Index(val value: Int) {
        companion object {
            fun initial(): Index = Index(0)

            fun fromUrl(url: String?): Index? {
                return url?.let { Url(url).parameters["offset"]?.toInt()?.let { Index(it) } }
            }
        }
    }
}
