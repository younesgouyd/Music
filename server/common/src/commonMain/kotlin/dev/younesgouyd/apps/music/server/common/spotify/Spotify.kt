package dev.younesgouyd.apps.music.server.common.spotify

import dev.younesgouyd.apps.music.common.models.SpotifyAuthState
import dev.younesgouyd.apps.music.common.models.spotify.Album
import dev.younesgouyd.apps.music.common.models.spotify.Artist
import dev.younesgouyd.apps.music.common.models.spotify.SearchResult
import dev.younesgouyd.apps.music.common.models.spotify.Track
import dev.younesgouyd.apps.music.common.models.spotify.common.AlbumId
import dev.younesgouyd.apps.music.common.models.spotify.common.ArtistId
import dev.younesgouyd.apps.music.server.common.spotify.repoes.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Clock

typealias ApiRespJson = String

class Spotify(
    tokensFile: File,
    credentialsFile: File
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

    private val tokenSaver = TokenSaver(tokensFile)
    private val credentialsSaver = CredentialsSaver(credentialsFile)

    private val authRepo = AuthRepo(authClient, tokenSaver, credentialsSaver)
    private val albumRepo = AlbumRepo(client, serializer, authRepo::getToken)
    private val artistRepo = ArtistRepo(client, serializer, authRepo::getToken)
    private val searchRepo = SearchRepo(client, serializer, authRepo::getToken)
    private val trackRepo = TrackRepo(client, serializer, authRepo::getToken)

    suspend fun getAuthState(): SpotifyAuthState {
        val credentials = credentialsSaver.load()
        return SpotifyAuthState(
            clientId = credentials?.clientId,
            clientSecret = credentials?.clientSecret,
            isAuthorized = isAuthorized()
        )
    }

    suspend fun isAuthorized(): Boolean {
        return authRepo.isAuthorized()
    }

    suspend fun getAuthorization(clientId: String, clientSecret: String) {
        authRepo.getAuthorization(clientId, clientSecret)
    }

    suspend fun deauthorize() {
        authRepo.deauthorize()
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

    class CredentialsSaver(private val credentialsFile: File) {
        suspend fun save(credentials: Credentials) {
            withContext(Dispatchers.IO) {
                credentialsFile.writeText(Json.encodeToString(credentials))
            }
        }

        suspend fun load(): Credentials? {
            return withContext(Dispatchers.IO) {
                runCatching {
                    Json.decodeFromString<Credentials>(credentialsFile.readText())
                }.getOrNull()
            }
        }

        suspend fun clear() {
            withContext(Dispatchers.IO) {
                credentialsFile.delete()
            }
        }
    }

    class TokenSaver(private val tokensFile: File) {
        suspend fun save(token: Token) {
            withContext(Dispatchers.IO) {
                tokensFile.writeText(Json.encodeToString(token))
            }
        }

        suspend fun load(): Token? {
            return withContext(Dispatchers.IO) {
                runCatching {
                    Json.decodeFromString<Token>(tokensFile.readText())
                }.getOrNull()
            }
        }

        suspend fun clear() {
            withContext(Dispatchers.IO) {
                tokensFile.delete()
            }
        }
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