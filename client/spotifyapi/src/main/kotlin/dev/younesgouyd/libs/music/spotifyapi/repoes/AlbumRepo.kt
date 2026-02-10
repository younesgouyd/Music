package dev.younesgouyd.libs.music.spotifyapi.repoes

import dev.younesgouyd.libs.music.spotifyapi.ApiRespJson
import dev.younesgouyd.libs.music.spotifyapi.models.Album
import dev.younesgouyd.libs.music.spotifyapi.models.common.AlbumId
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json

internal class AlbumRepo(
    private val client: HttpClient,
    private val serializer: Json,
    private val getToken: suspend () -> String
) {
    /**
     * GET /albums/{id}
     * @param id The Spotify ID of the album
     */
    suspend fun get(id: AlbumId): Pair<ApiRespJson, Album> {
        val response = client.get("albums/$id") {
            header("Authorization", "Bearer ${getToken()}")
        }.bodyAsText()
        return response to serializer.decodeFromString<Album>(response)
    }
}
