package dev.younesgouyd.apps.music.client.usecases

import dev.younesgouyd.apps.music.common.Inspection

expect class ImportLocalFileUseCase(
    mediaFileRepo: dev.younesgouyd.apps.music.client.data.repoes.MediaFileRepo,
    saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase
) {
    val mediaFileRepo: dev.younesgouyd.apps.music.client.data.repoes.MediaFileRepo
    val saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase
    suspend fun execute(inspection: Inspection.ItemInspection.LocalFileTrack, importSessionItemId: Long): Long
}
