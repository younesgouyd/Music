package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Server
import dev.younesgouyd.apps.music.common.models.SpotifyAuthState
import dev.younesgouyd.apps.music.common.models.rpc.SpotifyAuthRpc

class SpotifyAuthRepo(
    private val server: Server
) {
    suspend fun getAuthState(): SpotifyAuthState {
        return server.callForResult(SpotifyAuthRpc.GetAuthState)
    }

    suspend fun authorize(clientId: String, clientSecret: String) {
        server.call(SpotifyAuthRpc.Authorize(clientId, clientSecret))
    }

    suspend fun deauthorize() {
        server.call(SpotifyAuthRpc.Deauthorize)
    }
}