package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.SpotifyAuthState
import dev.younesgouyd.apps.music.common.models.rpc.SpotifyAuthRpc

class SpotifyAuthRepo(
    private val server: Server
) {
    suspend fun getAuthState(): SpotifyAuthState {
        return server.call(SpotifyAuthRpc.GetAuthState)
    }

    suspend fun authorize(clientId: String, clientSecret: String) {
        server.call<Unit>(SpotifyAuthRpc.Authorize(clientId, clientSecret))
    }

    suspend fun deauthorize() {
        server.call<Unit>(SpotifyAuthRpc.Deauthorize)
    }
}