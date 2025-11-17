package dev.younesgouyd.apps.music.client.usecases

import android.content.Context
import androidx.core.net.toUri
import dev.younesgouyd.apps.music.client.MusicAndroidApp
import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.data.repoes.MediaFileTrackCrossRefRepo
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class ImportLocalFileUseCaseImpl actual constructor(
    mediaFileRepo: MediaFileRepo,
    mediaFileTrackCrossRefRepo: MediaFileTrackCrossRefRepo,
    saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase
) : ImportLocalFileUseCase(
    mediaFileRepo,
    mediaFileTrackCrossRefRepo,
    saveAudioFileAsTrackUseCase
) {
    private val context: Context = MusicAndroidApp.instance.applicationContext

    override suspend fun execute(
        inspection: Inspection.ItemInspection.LocalFileTrack,
        importSessionItemId: ImportSessionItemId,
        folderId: FolderId?
    ): TrackId {
        return withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(inspection.uri.toUri())
                .use {
                    return@use import(
                        inspection = inspection,
                        importSessionItemId = importSessionItemId,
                        data = it!!,
                        folderId = folderId
                    )
                }
        }
    }
}