package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.rpc.MediaFileRpc
import java.io.File

class MediaFileRepo(
    private val server: Server
) {
    suspend fun getImportSessionImage(id: ImportSessionId): File? {
        val id = server.call<MediaFileId?>(MediaFileRpc.GetImportSessionImage(id)) ?: return null
        return server.getFile(id)
    }

    suspend fun getImportSessionItemImage(id: ImportSessionItemId): File? {
        val id = server.call<MediaFileId?>(MediaFileRpc.GetImportSessionItemImage(id)) ?: return null
        return server.getFile(id)
    }

    suspend fun getSpotifyAlbumImage(id: SpotifyAlbumId): File? {
        val id = server.call<MediaFileId?>(MediaFileRpc.GetSpotifyAlbumImage(id)) ?: return null
        return server.getFile(id)
    }

    suspend fun getSpotifyArtistImage(id: SpotifyArtistId): File? {
        val id = server.call<MediaFileId?>(MediaFileRpc.GetSpotifyArtistImage(id)) ?: return null
        return server.getFile(id)
    }

    suspend fun getImportSessionItemAudioUri(id: ImportSessionItemId): String? {
        val id = server.call<MediaFileId?>(MediaFileRpc.GetImportSessionItemAudio(id)) ?: return null
        return server.getFile(id)
            .toPath()
            .toUri()
            .toString()
    }
}