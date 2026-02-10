package dev.younesgouyd.apps.music.client.usecases

import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.data.repoes.TrackRepo
import java.io.InputStream

abstract class ImportLocalFileUseCase(
    private val trackRepo: TrackRepo,
    private val mediaFileRepo: MediaFileRepo
) {
    abstract suspend fun execute(
        uri: String,
        importSessionItemId: ImportSessionItemId,
        folderId: FolderId
    ): Pair<TrackId, MediaFileId>

    protected abstract fun getFileName(uri: String): String

    protected suspend fun import(
        uri: String,
        importSessionItemId: ImportSessionItemId,
        data: InputStream,
        folderId: FolderId
    ): Pair<TrackId, MediaFileId> {
        val trackId = trackRepo.add(
            importSessionItemId = importSessionItemId,
            spotifyTrackId = null,
            folderId = folderId
        )
        val audioMediaFileId = mediaFileRepo.add(
            fileName = getFileName(uri),
            data = data
        )
        return Pair(trackId, audioMediaFileId)
    }
}

expect class ImportLocalFileUseCaseImpl(
    trackRepo: TrackRepo,
    mediaFileRepo: MediaFileRepo
) : ImportLocalFileUseCase
