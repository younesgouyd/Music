package dev.younesgouyd.apps.music.client.usecases

import dev.younesgouyd.apps.music.client.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.common.Inspection

expect class ImportLocalFileUseCase(
    mediaFileRepo: MediaFileRepo,
    saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase
) {
    val mediaFileRepo: MediaFileRepo
    val saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase
    suspend fun execute(
        inspection: Inspection.ItemInspection.LocalFileTrack,
        importSessionItemId: Long,
        folderId: Long?
    ): Long
}
