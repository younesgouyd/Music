package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import dev.younesgouyd.apps.music.common.models.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.models.SpotifyArtistId
import kotlinx.serialization.Serializable

@Serializable
sealed class MediaFileRpc : Rpc() {
    @Serializable
    data class GetImportSessionImage(
        val id: ImportSessionId
    ) : MediaFileRpc()

    @Serializable
    data class GetImportSessionItemImage(
        val id: ImportSessionItemId
    ) : MediaFileRpc()

    @Serializable
    data class GetSpotifyAlbumImage(
        val id: SpotifyAlbumId
    ) : MediaFileRpc()

    @Serializable
    data class GetSpotifyArtistImage(
        val id: SpotifyArtistId
    ) : MediaFileRpc()

    @Serializable
    data class GetImportSessionItemAudio(
        val id: ImportSessionItemId
    ) : MediaFileRpc()
}