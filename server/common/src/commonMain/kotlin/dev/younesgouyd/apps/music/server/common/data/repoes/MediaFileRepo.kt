package dev.younesgouyd.apps.music.server.common.data.repoes

import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.server.common.data.room.daos.MediaFileDao
import kotlinx.coroutines.flow.first

class MediaFileRepo(
    private val dao: MediaFileDao
) {
    suspend fun getImportSessionImage(id: ImportSessionId): MediaFileId? {
        val mediaFile = dao.getImportSessionImage(id).first() ?: return null
        return mediaFile.id
    }

    suspend fun getImportSessionItemImage(id: ImportSessionItemId): MediaFileId? {
        val mediaFile = dao.getImportSessionItemImage(id).first() ?: return null
        return mediaFile.id
    }

    suspend fun getSpotifyAlbumImage(id: SpotifyAlbumId): MediaFileId? {
        val mediaFile = dao.getSpotifyAlbumImage(id).first() ?: return null
        return mediaFile.id
    }

    suspend fun getSpotifyArtistImage(id: SpotifyArtistId): MediaFileId? {
        val mediaFile = dao.getSpotifyArtistImage(id).first() ?: return null
        return mediaFile.id
    }

    suspend fun getImportSessionItemAudio(id: ImportSessionItemId): MediaFileId? {
        val mediaFile = dao.getImportSessionItemAudio(id).first() ?: return null
        return mediaFile.id
    }
}