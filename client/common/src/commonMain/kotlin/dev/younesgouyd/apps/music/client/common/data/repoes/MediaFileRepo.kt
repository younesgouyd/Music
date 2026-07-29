package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.common.ImportSessionId
import dev.younesgouyd.apps.music.common.ImportSessionItemId
import dev.younesgouyd.apps.music.common.SpotifyAlbumId
import dev.younesgouyd.apps.music.common.SpotifyArtistId
import io.ktor.client.*
import java.io.File

class MediaFileRepo(
    private val client: HttpClient
) {
    suspend fun getImportSessionImage(id: ImportSessionId): File? {
        TODO()
    }

    suspend fun getImportSessionItemImage(id: ImportSessionItemId): File? {
        TODO()
    }

    suspend fun getSpotifyAlbumImage(id: SpotifyAlbumId): File? {
        TODO()
    }

    suspend fun getSpotifyArtistImage(id: SpotifyArtistId): File? {
        TODO()
    }

    suspend fun getImportSessionItemAudioUri(id: ImportSessionItemId): String? {
        TODO()
    }
}