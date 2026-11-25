package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import dev.younesgouyd.apps.music.common.models.SpotifyTrackId
import dev.younesgouyd.apps.music.common.models.datamodels.SpotifyArtist
import kotlinx.coroutines.flow.Flow

class SpotifyArtistRepo(
    private val server: Server
) {
    fun get(id: SpotifyArtistId): Flow<SpotifyArtist?> {
        return server.observe(Rpc.ToServer.SpotifyArtist.Get(id))
    }

    fun getSpotifyTrackSpotifyArtists(id: SpotifyTrackId): Flow<List<SpotifyArtist>> {
        return server.observe(Rpc.ToServer.SpotifyArtist.GetSpotifyTrackSpotifyArtists(id))
    }

    fun getSpotifyAlbumSpotifyArtists(id: SpotifyAlbumId): Flow<List<SpotifyArtist>> {
        return server.observe(Rpc.ToServer.SpotifyArtist.GetSpotifyAlbumSpotifyArtists(id))
    }
}