package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.*
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFileDao
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.InputStream

class MediaFileRepo(
    private val dao: MediaFileDao,
    private val fileManager: FileManager
) {
    suspend fun getImportSessionImage(id: ImportSessionId): File? {
        val mediaFile = dao.getImportSessionImage(id).first() ?: return null
        return fileManager.getMediaFile(mediaFile.id)
    }

    suspend fun getImportSessionItemImage(id: ImportSessionItemId): File? {
        val mediaFile = dao.getImportSessionItemImage(id).first() ?: return null
        return fileManager.getMediaFile(mediaFile.id)
    }

    suspend fun getSpotifyAlbumImage(id: SpotifyAlbumId): File {
        val mediaFile = dao.getSpotifyAlbumImage(id).first()
        return fileManager.getMediaFile(mediaFile.id)
    }

    suspend fun getSpotifyArtistImage(id: SpotifyArtistId): File {
        val mediaFile = dao.getSpotifyArtistImage(id).first()
        return fileManager.getMediaFile(mediaFile.id)
    }

    suspend fun getImportSessionItemAudioUri(id: ImportSessionItemId): String? {
        val mediaFile = dao.getImportSessionItemAudio(id).first() ?: return null
        return fileManager.getMediaFile(mediaFile.id)
            .toPath()
            .toUri()
            .toString()
    }

    suspend fun add(fileName: String, data: InputStream): MediaFileId {
        val id = MediaFileId(
            value = dao.add(
                fileName = fileName,
                creationDatetime = System.currentTimeMillis()
            )
        )
        fileManager.saveMediaFile(data, id)
        return id
    }

    suspend fun add(fileName: String?, data: ByteArray): MediaFileId {
        val id = MediaFileId(
            value = dao.add(
                fileName = fileName,
                creationDatetime = System.currentTimeMillis()
            )
        )
        fileManager.saveMediaFile(data, id)
        return id
    }
}