package dev.younesgouyd.apps.music.client.data.repoes

import dev.younesgouyd.apps.music.client.data.*
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFile
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFileDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.InputStream

class MediaFileRepo(
    private val dao: MediaFileDao,
    private val fileManager: FileManager
) {
    fun getAll(): Flow<List<MediaFile>> {
        return dao.getAll()
    }

    suspend fun getImportSessionImageMediaFile(importSessionId: ImportSessionId): MediaFile? {
        return dao.getImportSessionMediaFiles(importSessionId = importSessionId, type = MediaFile.Type.Image)
            .map { it.firstOrNull() }
            .first()
    }

    suspend fun getImportSessionItemImageMediaFile(importSessionItemId: ImportSessionItemId): MediaFile? {
        return dao.getImportSessionItemMediaFiles(importSessionItemId = importSessionItemId, type = MediaFile.Type.Image)
            .map { it.firstOrNull() }
            .first()
    }

    suspend fun getTrackAudioUri(trackId: TrackId): String {
        val mediaFile = dao.getTrackMediaFiles(trackId = trackId, type = MediaFile.Type.Audio)
            .map { it.first() }
            .first()
        return fileManager.getMediaFile(mediaFile.id)
            .toPath()
            .toUri()
            .toString()
    }

    suspend fun getTrackImage(trackId: TrackId): File? {
        val mediaFile = dao.getTrackMediaFiles(trackId = trackId, type = MediaFile.Type.Image)
            .map { it.firstOrNull() }
            .first()
        if (mediaFile == null) return null
        return fileManager.getMediaFile(mediaFile.id)
    }

    suspend fun getImportSessionImage(importSessionId: ImportSessionId): File? {
        val mediaFile = dao.getImportSessionMediaFiles(importSessionId = importSessionId, type = MediaFile.Type.Image)
            .map { it.firstOrNull() }
            .first()
        if (mediaFile == null) return null
        return fileManager.getMediaFile(mediaFile.id)
    }

    suspend fun getImportSessionItemImage(importSessionItemId: ImportSessionItemId): File? {
        val mediaFile = dao.getImportSessionItemMediaFiles(importSessionItemId = importSessionItemId, type = MediaFile.Type.Image)
            .map { it.firstOrNull() }
            .first()
        if (mediaFile == null) return null
        return fileManager.getMediaFile(mediaFile.id)
    }

    suspend fun getArtistImage(artistId: ArtistId): File? {
        val mediaFile = dao.getArtistMediaFiles(artistId = artistId, type = MediaFile.Type.Image)
            .map { it.firstOrNull() }
            .first()
        if (mediaFile == null) return null
        return fileManager.getMediaFile(mediaFile.id)
    }

    suspend fun getPlaylistImage(playlistId: PlaylistId): File? {
        val mediaFile = dao.getPlaylistMediaFiles(playlistId = playlistId, type = MediaFile.Type.Image)
            .map { it.firstOrNull() }
            .first()
        if (mediaFile == null) return null
        return fileManager.getMediaFile(mediaFile.id)
    }

    suspend fun add(type: MediaFile.Type, fileName: String, data: InputStream): MediaFileId {
        val currentTime = System.currentTimeMillis()
        val id = MediaFileId(
            value = dao.add(
                type = type,
                fileName = fileName,
                creationDatetime = currentTime,
                updateDatetime = currentTime
            )
        )
        fileManager.saveMediaFile(data, id)
        return id
    }

    suspend fun add(type: MediaFile.Type, fileName: String?, data: ByteArray): MediaFileId{
        val currentTime = System.currentTimeMillis()
        val id = MediaFileId(
            value = dao.add(
                type = type,
                fileName = fileName,
                creationDatetime = currentTime,
                updateDatetime = currentTime
            )
        )
        fileManager.saveMediaFile(data, id)
        return id
    }
}