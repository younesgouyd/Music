package dev.younesgouyd.apps.music.server.common.spotify.repoes

import dev.younesgouyd.apps.music.common.models.spotify.SpotifyToken
import dev.younesgouyd.apps.music.server.common.spotify.InvalidCredentials
import dev.younesgouyd.apps.music.server.common.spotify.Spotify
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.time.Clock

internal class AuthRepo(
    private val httpClient: HttpClient,
    private val tokenSaver: Spotify.TokenSaver,
    private val credentialsSaver: Spotify.CredentialsSaver
) {
    private var credentials: Spotify.Credentials? = null
    private var token: Spotify.Token? = null

    suspend fun isAuthorized(): Boolean {
        if (token == null) {
            token = tokenSaver.load() ?: return false
            return (token?.expired() == false)
        }
        return token?.expired() == false
    }

    suspend fun getAuthorization(clientId: String, clientSecret: String) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw InvalidCredentials()
        }
        credentialsSaver.save(Spotify.Credentials(clientId = clientId, clientSecret = clientSecret))
        this.credentials = credentialsSaver.load()
        refreshToken()
    }

    // TODO
    suspend fun getToken(): String {
        if (!isAuthorized()) {
            if (credentials == null) {
                credentials = credentialsSaver.load()!!
                getAuthorization(credentials!!.clientId, credentials!!.clientSecret)
            } else {
                refreshToken()
            }
        }
        return token!!.accessToken
    }

    suspend fun deauthorize() {
        credentials = null
        token = null
        coroutineScope {
            launch { credentialsSaver.clear() }
            launch { tokenSaver.clear() }
        }
    }

    private suspend fun refreshToken() {
        val data = getTokenFromSpotify()
        if (data.isSuccess) {
            val spotifyToken: SpotifyToken = data.getOrThrow()
            tokenSaver.save(
                Spotify.Token(
                    accessToken = spotifyToken.accessToken,
                    expiresIn = spotifyToken.expiresIn,
                    receivedAt = Clock.System.now().epochSeconds
                )
            )
            token = tokenSaver.load()
        }
    }

    private suspend fun getTokenFromSpotify(): Result<SpotifyToken> {
        return try {
            val credentials = this.credentials!!
            val response = httpClient.post {
                url("https://accounts.spotify.com/api/token")
                header("Authorization", "Basic ${Base64.encode("${credentials.clientId}:${credentials.clientSecret}".toByteArray())}")
                header("Content-Type", "application/x-www-form-urlencoded")
                setBody("grant_type=client_credentials")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body<SpotifyToken>())
            } else {
                Result.failure(Exception(response.bodyAsText()))
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
