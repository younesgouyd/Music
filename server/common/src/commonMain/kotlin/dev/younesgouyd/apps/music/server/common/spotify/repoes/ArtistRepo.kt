package dev.younesgouyd.apps.music.server.common.spotify.repoes

import dev.younesgouyd.apps.music.common.spotifyapimodels.Artist
import dev.younesgouyd.apps.music.common.spotifyapimodels.common.ArtistId
import dev.younesgouyd.apps.music.server.common.spotify.ApiRespJson
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json

internal class ArtistRepo(
    private val client: HttpClient,
    private val serializer: Json,
    private val getToken: suspend () -> String
) {
    suspend fun get(artistIds: List<ArtistId>): List<Pair<ApiRespJson, Artist>> {
        return buildList {
            for (id in artistIds) {
                add(get(id))
            }
        }
    }

    /**
     * GET /artists/{id}
     * @param id The Spotify ID of the artist
     */
    suspend fun get(id: ArtistId): Pair<ApiRespJson, Artist> {
        val response = client.get("artists/$id") {
            header("Authorization", "Bearer ${getToken()}")
        }.bodyAsText()
        return response to serializer.decodeFromString<Artist>(response)
    }
}