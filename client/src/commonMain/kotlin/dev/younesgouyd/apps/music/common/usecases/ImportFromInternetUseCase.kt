package dev.younesgouyd.apps.music.common.usecases

import dev.younesgouyd.apps.music.common.Inspection
import dev.younesgouyd.apps.music.common.data.Server
import dev.younesgouyd.apps.music.common.data.repoes.MediaFileRepo
import kotlinx.coroutines.flow.first
import kotlin.io.encoding.Base64

class ImportFromInternetUseCase(
    val mediaFileRepo: MediaFileRepo,
    val server: Server,
    val saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase
) {
    suspend fun execute(inspection: Inspection.ItemInspection.InternetTrack, importSessionItemId: Long): Boolean {
        val result: String = server.download(inspection.uri).first()
        return when (result) {
            "error" -> false
            "completed" -> {
                val data = server.getResult()
                import(
                    inspection = inspection,
                    importSessionItemId = importSessionItemId,
                    data = data
                )
                return true // TODO
            }
            else -> TODO()
        }
    }

    private suspend fun import(
        inspection: Inspection.ItemInspection.InternetTrack,
        importSessionItemId: Long,
        data: ByteArray
    ) {
        val trackId = saveAudioFileAsTrackUseCase.execute(
            folderId = null,
            title = inspection.title,
            duration = inspection.duration,
            artists = inspection.artists,
            album = inspection.album,
            releaseYear = null, // TODO
            albumTrackNumber = null, // TODO
            lyrics = null, // TODO
            albumImage = inspection.thumbnail?.let { Base64.decode(it) }
        )
        mediaFileRepo.add(
            trackId = trackId,
            importSessionItemId = importSessionItemId,
            data = data
        )
    }
}
