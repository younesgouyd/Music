package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.FileManager
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFile
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFileDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.InputStream

class MediaFileRepo(
    private val dao: MediaFileDao,
    private val fileManager: FileManager
) {
    suspend fun getImportSessionImageMediaFile(importSessionId: Long): MediaFile? {
        return dao.getImportSessionMediaFiles(importSessionId = importSessionId, type = MediaFile.Type.Image)
            .map { it.firstOrNull() }
            .first()
    }

    suspend fun getImportSessionItemImageMediaFile(importSessionItemId: Long): MediaFile? {
        return dao.getImportSessionItemMediaFiles(importSessionItemId = importSessionItemId, type = MediaFile.Type.Image)
            .map { it.firstOrNull() }
            .first()
    }

    suspend fun getTrackAudioUri(trackId: Long): String {
        val mediaFile = dao.getTrackMediaFiles(trackId = trackId, type = MediaFile.Type.Audio)
            .map { it.first() }
            .first()
        return fileManager.getMediaFile(mediaFile.id)
            .toPath()
            .toUri()
            .toString()
    }

    suspend fun getTrackImage(trackId: Long): File? {
        val mediaFile = dao.getTrackMediaFiles(trackId = trackId, type = MediaFile.Type.Image)
            .map { it.firstOrNull() }
            .first()
        if (mediaFile == null) return null
        return fileManager.getMediaFile(mediaFile.id)
    }

    suspend fun getImportSessionImage(importSessionId: Long): File? {
        val mediaFile = dao.getImportSessionMediaFiles(importSessionId = importSessionId, type = MediaFile.Type.Image)
            .map { it.firstOrNull() }
            .first()
        if (mediaFile == null) return null
        return fileManager.getMediaFile(mediaFile.id)
    }

    suspend fun getImportSessionItemImage(importSessionItemId: Long): File? {
        val mediaFile = dao.getImportSessionItemMediaFiles(importSessionItemId = importSessionItemId, type = MediaFile.Type.Image)
            .map { it.firstOrNull() }
            .first()
        if (mediaFile == null) return null
        return fileManager.getMediaFile(mediaFile.id)
    }

    suspend fun getArtistImage(artistId: Long): File? {
        val mediaFile = dao.getArtistMediaFiles(artistId = artistId, type = MediaFile.Type.Image)
            .map { it.firstOrNull() }
            .first()
        if (mediaFile == null) return null
        return fileManager.getMediaFile(mediaFile.id)
    }

    suspend fun getPlaylistImage(playlistId: Long): File? {
        val mediaFile = dao.getPlaylistMediaFiles(playlistId = playlistId, type = MediaFile.Type.Image)
            .map { it.firstOrNull() }
            .first()
        if (mediaFile == null) return null
        return fileManager.getMediaFile(mediaFile.id)
    }

    suspend fun add(type: MediaFile.Type, data: InputStream): Long {
        val currentTime = System.currentTimeMillis()
        val id = dao.add(
            type = type,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
        fileManager.saveMediaFile(data, id)
        return id
    }

    suspend fun add(type: MediaFile.Type, data: ByteArray): Long {
        val currentTime = System.currentTimeMillis()
        val id = dao.add(
            type = type,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
        fileManager.saveMediaFile(data, id)
        return id
    }
}