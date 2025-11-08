package dev.younesgouyd.apps.music.client.usecases

import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.flow.first
import java.io.InputStream

class ImportFromInternetUseCase(
    val mediaFileRepo: dev.younesgouyd.apps.music.client.data.repoes.MediaFileRepo,
    val server: dev.younesgouyd.apps.music.client.data.Server,
    val saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase
) {
    suspend fun execute(
        inspection: Inspection.ItemInspection.InternetTrack,
        importSessionItemId: Long,
        folderId: Long?
    ): Long? {
        val result: String = server.download(inspection.uri).first()
        return when (result) {
            "error" -> null
            "completed" -> {
                server.getResult().use {
                    return@use import(
                        inspection = inspection,
                        importSessionItemId = importSessionItemId,
                        data = it,
                        folderId = folderId
                    )
                }
            }

            else -> TODO()
        }
    }

    private suspend fun import(
        inspection: Inspection.ItemInspection.InternetTrack,
        importSessionItemId: Long,
        data: InputStream,
        folderId: Long?
    ): Long {
        val trackId = saveAudioFileAsTrackUseCase.execute(
            folderId = folderId,
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
