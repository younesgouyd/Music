package dev.younesgouyd.apps.music.client.usecases

import dev.younesgouyd.apps.music.client.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.data.repoes.MediaFileTrackCrossRefRepo
import dev.younesgouyd.apps.music.common.Inspection
import java.net.URI
import kotlin.io.path.toPath

actual class ImportLocalFileUseCaseImpl actual constructor(
    mediaFileRepo: MediaFileRepo,
    mediaFileTrackCrossRefRepo: MediaFileTrackCrossRefRepo,
    saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase
) : ImportLocalFileUseCase(
    mediaFileRepo,
    mediaFileTrackCrossRefRepo,
    saveAudioFileAsTrackUseCase
) {
    override suspend fun execute(
        inspection: Inspection.ItemInspection.LocalFileTrack,
        importSessionItemId: Long,
        folderId: Long?
    ): Long {
        URI(inspection.uri)
            .toPath()
            .toFile()
            .inputStream().use {
                return import(
                    inspection = inspection,
                    importSessionItemId = importSessionItemId,
                    data = it,
                    folderId = folderId
                )
            }
    }
}