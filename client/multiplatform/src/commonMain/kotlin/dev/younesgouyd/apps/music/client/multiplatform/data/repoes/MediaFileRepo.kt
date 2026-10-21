package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.*
import java.io.File

class MediaFileRepo(
    private val server: Server
) {
    suspend fun getImportSessionImage(id: ImportSessionId): File? {
        val id = server.get(Rpc.ToServer.MediaFile.GetImportSessionImage(id)) ?: return null
        return server.getFile(id)
    }

    suspend fun getImportSessionItemImage(id: ImportSessionItemId): File? {
        val id = server.get(Rpc.ToServer.MediaFile.GetImportSessionItemImage(id)) ?: return null
        return server.getFile(id)
    }

    suspend fun getSpotifyAlbumImage(id: SpotifyAlbumId): File? {
        val id = server.get(Rpc.ToServer.MediaFile.GetSpotifyAlbumImage(id)) ?: return null
        return server.getFile(id)
    }

    suspend fun getSpotifyArtistImage(id: SpotifyArtistId): File? {
        val id = server.get(Rpc.ToServer.MediaFile.GetSpotifyArtistImage(id)) ?: return null
        return server.getFile(id)
    }

    suspend fun getImportSessionItemAudioUri(id: ImportSessionItemId): String? {
        val id = server.get(Rpc.ToServer.MediaFile.GetImportSessionItemAudio(id)) ?: return null
        return server.getFile(id)
            .toPath()
            .toUri()
            .toString()
    }
}