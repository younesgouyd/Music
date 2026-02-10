package dev.younesgouyd.apps.music.client.usecases

import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.data.repoes.TrackRepo
import java.net.URI
import kotlin.io.path.toPath

actual class ImportLocalFileUseCaseImpl actual constructor(
    trackRepo: TrackRepo,
    mediaFileRepo: MediaFileRepo
) : ImportLocalFileUseCase(
    trackRepo,
    mediaFileRepo,
) {
    override fun getFileName(uri: String): String {
        return URI(uri).toPath().toFile().name
    }

    override suspend fun execute(
        uri: String,
        importSessionItemId: ImportSessionItemId,
        folderId: FolderId
    ): Pair<TrackId, MediaFileId> {
        URI(uri)
            .toPath()
            .toFile()
            .inputStream().use {
                return import(
                    uri = uri,
                    importSessionItemId = importSessionItemId,
                    data = it,
                    folderId = folderId
                )
            }
    }
}