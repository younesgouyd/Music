package dev.younesgouyd.apps.music.client.usecases

import dev.younesgouyd.apps.music.client.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.data.repoes.MediaFileTrackCrossRefRepo
import dev.younesgouyd.apps.music.client.data.room.entities.MediaFile
import dev.younesgouyd.apps.music.common.Inspection
import java.io.InputStream

abstract class ImportLocalFileUseCase(
    val mediaFileRepo: MediaFileRepo,
    val mediaFileTrackCrossRefRepo: MediaFileTrackCrossRefRepo,
    val saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase
) {
    abstract suspend fun execute(
        inspection: Inspection.ItemInspection.LocalFileTrack,
        importSessionItemId: Long,
        folderId: Long?
    ): Long

    protected suspend fun import(
        inspection: Inspection.ItemInspection.LocalFileTrack,
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
            albumTrackNumber = inspection.albumTrackNumber,
            lyrics = inspection.lyrics
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
            data = data
        )
        mediaFileTrackCrossRefRepo.add(
            mediaFileId = audioMediaFileId,
            trackId = trackId
        )
        return trackId
    }
}

expect class ImportLocalFileUseCaseImpl(
    mediaFileRepo: MediaFileRepo,
    mediaFileTrackCrossRefRepo: MediaFileTrackCrossRefRepo,
    saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase
) : ImportLocalFileUseCase
