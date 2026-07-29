package dev.younesgouyd.apps.music.server.common.spotify

import dev.younesgouyd.apps.music.common.spotifyapimodels.Album
import dev.younesgouyd.apps.music.common.spotifyapimodels.Artist
import dev.younesgouyd.apps.music.common.spotifyapimodels.SearchResult
import dev.younesgouyd.apps.music.common.spotifyapimodels.Track
import dev.younesgouyd.apps.music.common.spotifyapimodels.common.AlbumId
import dev.younesgouyd.apps.music.common.spotifyapimodels.common.ArtistId
import dev.younesgouyd.apps.music.server.common.spotify.repoes.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.job
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

typealias ApiRespJson = String

class Spotify(
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

    suspend fun close() {
        client.close()
        authClient.close()
        client.coroutineContext.job.join()
        authClient.coroutineContext.job.join()
    }

    abstract class TokenSaver {
        abstract suspend fun save(token: Token)
        abstract suspend fun load(): Token?
        abstract suspend fun clear()
    }

    @Serializable
    data class Credentials(
        val clientId: String,
        val clientSecret: String
    )

    @Serializable
    data class Token(
        val accessToken: String,
        val expiresIn: Int,
        val receivedAt: Long
    ) {
        fun expired() = Clock.System.now().epochSeconds >= receivedAt + expiresIn
    }
}