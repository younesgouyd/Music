package dev.younesgouyd.apps.music.common.usecases

import android.content.Context
import androidx.core.net.toUri
import dev.younesgouyd.apps.music.android.Music
import dev.younesgouyd.apps.music.common.Inspection
import dev.younesgouyd.apps.music.common.data.repoes.MediaFileRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64

actual class ImportLocalFileUseCase actual constructor(
    mediaFileRepo: MediaFileRepo,
    saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase
) {
    private val context: Context = Music.instance.applicationContext
    actual val mediaFileRepo: MediaFileRepo = mediaFileRepo
    actual val saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase = saveAudioFileAsTrackUseCase

    actual suspend fun execute(inspection: Inspection.ItemInspection.LocalFileTrack, importSessionItemId: Long): Boolean {
        val data = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(inspection.uri.toUri())
                .use { it!!.readBytes() }
        }
        import(
            inspection = inspection,
            importSessionItemId = importSessionItemId,
            data = data
        )
        return true
    }

    private suspend fun import(
        inspection: Inspection.ItemInspection.LocalFileTrack,
        importSessionItemId: Long,
        data: ByteArray
    ) {
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
    }
}