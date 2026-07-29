package dev.younesgouyd.apps.music.server.common.spotify.repoes

import dev.younesgouyd.apps.music.common.spotifyapimodels.SpotifyToken
import dev.younesgouyd.apps.music.server.common.spotify.InvalidCredentials
import dev.younesgouyd.apps.music.server.common.spotify.Spotify
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.io.encoding.Base64
import kotlin.time.Clock

internal class AuthRepo(
    private val httpClient: HttpClient,
    private val tokenSaver: Spotify.TokenSaver,
    private val getCredentials: suspend () -> Spotify.Credentials
) {
    private var clientId: String? = null
    private var clientSecret: String? = null
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
        this.clientId = clientId
        this.clientSecret = clientSecret
        refreshToken()
    }

    suspend fun getToken(): String {
        if (!isAuthorized()) {
            if (clientId == null || clientSecret == null) {
                val credentials = getCredentials()
                getAuthorization(credentials.clientId, credentials.clientSecret)
            } else {
                refreshToken()
            }
        }
        return token!!.accessToken
    }

    suspend fun clearToken() {
        token = null
        tokenSaver.clear()
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
            val response = httpClient.post {
                url("https://accounts.spotify.com/api/token")
                header("Authorization", "Basic ${Base64.encode("$clientId:$clientSecret".toByteArray())}")
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
