package dev.younesgouyd.apps.music.client.usecases

import dev.younesgouyd.apps.music.client.data.*
import dev.younesgouyd.apps.music.client.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.data.repoes.TrackRepo
import kotlinx.coroutines.flow.first
import java.io.InputStream

class ImportFromInternetUseCase(
    private val server: Server,
    private val trackRepo: TrackRepo,
    private val mediaFileRepo: MediaFileRepo
) {
    suspend fun execute(
        uri: String,
        importSessionItemId: ImportSessionItemId,
        folderId: FolderId
    ): Pair<TrackId, MediaFileId>? {
        val result: String = server.download(uri).first()
        return when (result) {
            "error" -> null
            "completed" -> {
                val (filename, stream) = server.getResult()
                stream.use {
                    import(
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
        importSessionItemId: ImportSessionItemId,
        fileName: String,
        data: InputStream,
        folderId: FolderId
    ): Pair<TrackId, MediaFileId> {
        val trackId = trackRepo.add(
            importSessionItemId = importSessionItemId,
            spotifyTrackId = null,
            folderId = folderId
        )
        val audioMediaFileId = mediaFileRepo.add(
            fileName = fileName,
            data = data
        )
        return Pair(trackId, audioMediaFileId)
    }
}
