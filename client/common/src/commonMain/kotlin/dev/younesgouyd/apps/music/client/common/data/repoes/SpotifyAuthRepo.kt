package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.SpotifyAuthState
import dev.younesgouyd.apps.music.common.models.rpc.SpotifyAuthRpc
import io.ktor.client.call.*

class SpotifyAuthRepo(
    private val backend: Backend
) {
    suspend fun getAuthState(): SpotifyAuthState {
        return backend.call(SpotifyAuthRpc.GetAuthState).body<SpotifyAuthState>()
    }

    suspend fun authorize(clientId: String, clientSecret: String) {
        backend.call(SpotifyAuthRpc.Authorize(clientId, clientSecret))
    }

    suspend fun deauthorize() {
        backend.call(SpotifyAuthRpc.Deauthorize)
    }
}