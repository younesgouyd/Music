package dev.younesgouyd.apps.music.common.usecases

import dev.younesgouyd.apps.music.common.Inspection
import dev.younesgouyd.apps.music.common.data.Server
import dev.younesgouyd.apps.music.common.data.repoes.MediaFileRepo
import kotlinx.coroutines.flow.first
import java.io.InputStream

class ImportFromInternetUseCase(
    val mediaFileRepo: MediaFileRepo,
    val server: Server,
    val saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase
) {
    suspend fun execute(inspection: Inspection.ItemInspection.InternetTrack, importSessionItemId: Long): Long? {
        val result: String = server.download(inspection.uri).first()
        return when (result) {
            "error" -> null
            "completed" -> {
                server.getResult().use {
                    return@use import(
                        inspection = inspection,
                        importSessionItemId = importSessionItemId,
                        data = it
                    )
                }
            }
            else -> TODO()
        }
    }

    private suspend fun import(
        inspection: Inspection.ItemInspection.InternetTrack,
        importSessionItemId: Long,
        data: InputStream
    ): Long {
        val trackId = saveAudioFileAsTrackUseCase.execute(
            folderId = null,
            title = inspection.title,
            duration = inspection.duration,
            artists = inspection.artists,
            album = inspection.album,
            albumTrackNumber = null, // TODO
            lyrics = null, // TODO
            albumImage = inspection.thumbnail
        )
        mediaFileRepo.add(
            trackId = trackId,
            importSessionItemId = importSessionItemId,
            data = data
        )
        return trackId
    }
}
