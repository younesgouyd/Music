package dev.younesgouyd.apps.music.client.usecases

import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.data.Server
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.data.repoes.MediaFileTrackCrossRefRepo
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFile
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.flow.first
import java.io.InputStream

class ImportFromInternetUseCase(
    val mediaFileRepo: MediaFileRepo,
    val mediaFileTrackCrossRefRepo: MediaFileTrackCrossRefRepo,
    val server: Server,
    val saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase
) {
    suspend fun execute(
        inspection: Inspection.ItemInspection.InternetTrack,
        importSessionItemId: ImportSessionItemId,
        folderId: FolderId?
    ): TrackId? {
        val result: String = server.download(inspection.uri).first()
        return when (result) {
            "error" -> null
            "completed" -> {
                val (filename, stream) = server.getResult()
                stream.use {
                    import(
                        inspection = inspection,
                        importSessionItemId = importSessionItemId,
                        fileName = filename,
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
        importSessionItemId: ImportSessionItemId,
        fileName: String,
        data: InputStream,
        folderId: FolderId?
    ): TrackId {
        val trackId = saveAudioFileAsTrackUseCase.execute(
            folderId = folderId,
            title = inspection.title,
            duration = inspection.duration,
            artists = inspection.artists,
            album = inspection.album,
            albumTrackNumber = null, // TODO
            lyrics = null // TODO
        )
        val imageMediaFile = mediaFileRepo.getImportSessionItemImageMediaFile(importSessionItemId = importSessionItemId)
        if (imageMediaFile != null) {
            mediaFileTrackCrossRefRepo.add(
                mediaFileId = imageMediaFile.id,
                trackId = trackId
            )
        }
        val audioMediaFileId = mediaFileRepo.add(
            type = MediaFile.Type.Audio,
            fileName = fileName,
            data = data
        )
        mediaFileTrackCrossRefRepo.add(
            mediaFileId = audioMediaFileId,
            trackId = trackId
        )
        return trackId
    }
}
