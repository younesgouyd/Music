package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.datamodels.SpotifyAuthState

class SpotifyAuthRepo(
    private val server: Server
) {
    suspend fun getAuthState(): SpotifyAuthState {
        return server.get(Rpc.ToServer.SpotifyAuth.GetAuthState)
    }

    suspend fun authorize(clientId: String, clientSecret: String) {
        server.call(Rpc.ToServer.SpotifyAuth.Authorize(clientId, clientSecret))
    }

    suspend fun deauthorize() {
        server.call(Rpc.ToServer.SpotifyAuth.Deauthorize)
    }
}