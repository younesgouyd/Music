package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.client.common.data.FileManager
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.rpc.MediaFileRpc
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class MediaFileRepo(
    private val backend: Backend,
    private val fileManager: FileManager
) {
    private val cache = ConcurrentHashMap<String, File>()

    suspend fun getImportSessionImage(id: ImportSessionId): File? {
        return getMediaFile("GetImportSessionImage(id=$id)") {
            backend.call(MediaFileRpc.GetImportSessionImage(id))
        }
    }

    suspend fun getImportSessionItemImage(id: ImportSessionItemId): File? {
        return getMediaFile("GetImportSessionItemImage(id=$id)") {
            backend.call(MediaFileRpc.GetImportSessionItemImage(id))
        }
    }

    suspend fun getSpotifyAlbumImage(id: SpotifyAlbumId): File? {
        return getMediaFile("GetSpotifyAlbumImage(id=$id)") {
            backend.call(MediaFileRpc.GetSpotifyAlbumImage(id))
        }
    }

    suspend fun getSpotifyArtistImage(id: SpotifyArtistId): File? {
        return getMediaFile("GetSpotifyArtistImage(id=$id)") {
            backend.call(MediaFileRpc.GetSpotifyArtistImage(id))
        }
    }

    suspend fun getImportSessionItemAudioUri(id: ImportSessionItemId): String? {
        return getMediaFile("GetImportSessionItemAudioUri(id=$id)") {
            backend.call(MediaFileRpc.GetImportSessionItemAudio(id))
        }?.toPath()
            ?.toUri()
            ?.toString()
    }

    private suspend inline fun getMediaFile(key: String, backendCall: () -> HttpResponse): File? {
        val file = cache[key]
        if (file != null) {
            return file
        }
        val response = backendCall()
        if (response.status.isSuccess()) {
            val stream = response.bodyAsChannel().toInputStream()
            val fileId = MediaFileId(response.headers["X-MEDIA-FILE-ID"]!!.toLong())
            val file = fileManager.storeTemp(fileId, stream)
            cache[key] = file
            return file
        }
        return null
    }
}