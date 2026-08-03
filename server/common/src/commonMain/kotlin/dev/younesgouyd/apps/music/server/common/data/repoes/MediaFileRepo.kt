package dev.younesgouyd.apps.music.server.common.data.repoes

import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.server.common.data.FileManager
import dev.younesgouyd.apps.music.server.common.data.room.daos.MediaFileDao
import kotlinx.coroutines.flow.first
import java.io.File

class MediaFileRepo(
    private val dao: MediaFileDao,
    private val fileManager: FileManager
) {
    suspend fun getImportSessionImage(id: ImportSessionId): Pair<MediaFileId, File>? {
        val mediaFile = dao.getImportSessionImage(id).first() ?: return null
        return Pair(mediaFile.id, fileManager.getMediaFile(mediaFile.id))
    }

    suspend fun getImportSessionItemImage(id: ImportSessionItemId): Pair<MediaFileId, File>? {
        val mediaFile = dao.getImportSessionItemImage(id).first() ?: return null
        return Pair(mediaFile.id, fileManager.getMediaFile(mediaFile.id))
    }

    suspend fun getSpotifyAlbumImage(id: SpotifyAlbumId): Pair<MediaFileId, File>? {
        val mediaFile = dao.getSpotifyAlbumImage(id).first() ?: return null
        return Pair(mediaFile.id, fileManager.getMediaFile(mediaFile.id))
    }

    suspend fun getSpotifyArtistImage(id: SpotifyArtistId): Pair<MediaFileId, File>? {
        val mediaFile = dao.getSpotifyArtistImage(id).first() ?: return null
        return Pair(mediaFile.id, fileManager.getMediaFile(mediaFile.id))
    }

    suspend fun getImportSessionItemAudio(id: ImportSessionItemId): Pair<MediaFileId, File>? {
        val mediaFile = dao.getImportSessionItemAudio(id).first() ?: return null
        return Pair(mediaFile.id, fileManager.getMediaFile(mediaFile.id))
    }
}