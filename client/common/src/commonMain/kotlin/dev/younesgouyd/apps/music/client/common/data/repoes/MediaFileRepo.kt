package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.rpc.MediaFileRpc
import java.io.File

class MediaFileRepo(
    private val backend: Backend
) {
    suspend fun getImportSessionImage(id: ImportSessionId): File? {
        val id = backend.callForResult<MediaFileId?>(MediaFileRpc.GetImportSessionImage(id)) ?: return null
        return backend.getFile(id)
    }

    suspend fun getImportSessionItemImage(id: ImportSessionItemId): File? {
        val id = backend.callForResult<MediaFileId?>(MediaFileRpc.GetImportSessionItemImage(id)) ?: return null
        return backend.getFile(id)
    }

    suspend fun getSpotifyAlbumImage(id: SpotifyAlbumId): File? {
        val id = backend.callForResult<MediaFileId?>(MediaFileRpc.GetSpotifyAlbumImage(id)) ?: return null
        return backend.getFile(id)
    }

    suspend fun getSpotifyArtistImage(id: SpotifyArtistId): File? {
        val id = backend.callForResult<MediaFileId?>(MediaFileRpc.GetSpotifyArtistImage(id)) ?: return null
        return backend.getFile(id)
    }

    suspend fun getImportSessionItemAudioUri(id: ImportSessionItemId): String? {
        val id = backend.callForResult<MediaFileId?>(MediaFileRpc.GetImportSessionItemAudio(id)) ?: return null
        return backend.getFile(id)
            .toPath()
            .toUri()
            .toString()
    }
}