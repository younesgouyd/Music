package dev.younesgouyd.apps.music.common.usecases

import dev.younesgouyd.apps.music.common.Inspection
import dev.younesgouyd.apps.music.common.data.repoes.MediaFileRepo
import java.io.InputStream
import java.net.URI
import kotlin.io.encoding.Base64
import kotlin.io.path.toPath

actual class ImportLocalFileUseCase actual constructor(
    mediaFileRepo: MediaFileRepo,
    saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase
) {
    actual val mediaFileRepo: MediaFileRepo = mediaFileRepo
    actual val saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase = saveAudioFileAsTrackUseCase

    actual suspend fun execute(inspection: Inspection.ItemInspection.LocalFileTrack, importSessionItemId: Long): Long {
        URI(inspection.uri)
            .toPath()
            .toFile()
            .inputStream().use {
                return import(
                    inspection = inspection,
                    importSessionItemId = importSessionItemId,
                    data = it
                )
            }
    }

    private suspend fun import(
        inspection: Inspection.ItemInspection.LocalFileTrack,
        importSessionItemId: Long,
        data: InputStream
    ): Long {
        val trackId = saveAudioFileAsTrackUseCase.execute(
            folderId = null,
            title = inspection.title,
            duration = inspection.duration,
            artists = inspection.artists,
            album = inspection.album,
            releaseYear = inspection.year,
            albumTrackNumber = inspection.albumTrackNumber,
            lyrics = inspection.lyrics,
            albumImage = inspection.albumImage?.let { Base64.decode(it) }
        )
        mediaFileRepo.add(
            trackId = trackId,
            importSessionItemId = importSessionItemId,
            data = data
        )
        return trackId
    }
}