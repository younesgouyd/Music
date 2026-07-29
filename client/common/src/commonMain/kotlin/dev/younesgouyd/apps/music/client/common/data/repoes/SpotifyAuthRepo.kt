package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.common.SpotifyAuthState
import io.ktor.client.*

class SpotifyAuthRepo(
    private val client: HttpClient
) {
    fun getAuthState(): SpotifyAuthState {
        TODO()
    }

    suspend fun updateCredentials(clientId: String, clientSecret: String) {
        TODO()
    }

    suspend fun authorize() {
        TODO()
    }

    suspend fun deauthorize() {
        TODO()
    }
}