package dev.younesgouyd.libs.music.spotifyapi

import dev.younesgouyd.libs.music.spotifyapi.models.Album
import dev.younesgouyd.libs.music.spotifyapi.models.Artist
import dev.younesgouyd.libs.music.spotifyapi.models.SearchResult
import dev.younesgouyd.libs.music.spotifyapi.models.Track
import dev.younesgouyd.libs.music.spotifyapi.models.common.AlbumId
import dev.younesgouyd.libs.music.spotifyapi.models.common.ArtistId
import dev.younesgouyd.libs.music.spotifyapi.repoes.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.time.Instant

typealias ApiRespJson = String

class SpotifyApi(
    tokenSaver: TokenSaver,
    getCredentials: suspend () -> Credentials
) {
    private val serializer = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(CIO) {
        install(Logging) { level = LogLevel.ALL }
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.spotify.com"
                path("v1/")
            }
        }
    }
    private val authClient = HttpClient(CIO) {
        install(Logging) { level = LogLevel.ALL }
        install(ContentNegotiation) { json(Json) }
    }
    private val authRepo = AuthRepo(authClient, tokenSaver, getCredentials)
    private val albumRepo = AlbumRepo(client, serializer, authRepo::getToken)
    private val artistRepo = ArtistRepo(client, serializer, authRepo::getToken)
    private val searchRepo = SearchRepo(client, serializer, authRepo::getToken)
    private val trackRepo = TrackRepo(client, serializer, authRepo::getToken)

    suspend fun isAuthorized(): Boolean {
        return authRepo.isAuthorized()
    }

    suspend fun getAuthorization(clientId: String, clientSecret: String) {
        authRepo.getAuthorization(clientId, clientSecret)
    }

    suspend fun clearToken() {
        authRepo.clearToken()
    }

    suspend fun search(track: String, artist: String?, album: String?, year: String?): SearchResult {
        return searchRepo.search(track, artist, album, year)
    }

    suspend fun getAlbumTracks(id: AlbumId): List<Pair<ApiRespJson, Track>> {
        return trackRepo.getAlbumTracks(id)
    }

    suspend fun getArtists(artistIds: List<ArtistId>): List<Pair<ApiRespJson, Artist>> {
        return artistRepo.get(artistIds)
    }

    suspend fun getAlbum(id: AlbumId): Pair<ApiRespJson, Album> {
        return albumRepo.get(id)
    }

    fun close() {
        client.close()
        authClient.close()
    }

    abstract class TokenSaver {
        abstract suspend fun save(token: Token)
        abstract suspend fun load(): Token?
        abstract suspend fun clear()
    }

    data class Credentials(
        val clientId: String,
        val clientSecret: String
    )

    data class Token(
        val accessToken: String,
        val expiresIn: Int,
        val receivedAt: Long
    ) {
        fun expired() = Instant.now().epochSecond >= receivedAt + expiresIn
    }
}