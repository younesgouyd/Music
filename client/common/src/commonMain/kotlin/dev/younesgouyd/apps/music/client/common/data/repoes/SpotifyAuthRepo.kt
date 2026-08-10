package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.SpotifyAuthState
import dev.younesgouyd.apps.music.common.models.rpc.SpotifyAuthRpc

class SpotifyAuthRepo(
    private val backend: Backend
) {
    suspend fun getAuthState(): SpotifyAuthState {
        return backend.callForResult(SpotifyAuthRpc.GetAuthState)
    }

    suspend fun authorize(clientId: String, clientSecret: String) {
        backend.call(SpotifyAuthRpc.Authorize(clientId, clientSecret))
    }

    suspend fun deauthorize() {
        backend.call(SpotifyAuthRpc.Deauthorize)
    }
}