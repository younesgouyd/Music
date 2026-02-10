package dev.younesgouyd.apps.music.client.usecases

import android.content.Context
import androidx.core.net.toUri
import dev.younesgouyd.apps.music.client.MusicAndroidApp
import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.ImportSessionItemId
import dev.younesgouyd.apps.music.client.data.MediaFileId
import dev.younesgouyd.apps.music.client.data.TrackId
import dev.younesgouyd.apps.music.client.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.client.getFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class ImportLocalFileUseCaseImpl actual constructor(
    trackRepo: TrackRepo,
    mediaFileRepo: MediaFileRepo
) : ImportLocalFileUseCase(
    trackRepo,
    mediaFileRepo
) {
    private val context: Context = MusicAndroidApp.instance.applicationContext

    override fun getFileName(uri: String): String {
        return getFileName(uri.toUri())
    }

    override suspend fun execute(
        uri: String,
        importSessionItemId: ImportSessionItemId,
        folderId: FolderId
    ): Pair<TrackId, MediaFileId> {
        return withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri.toUri())
                .use {
                    return@use import(
                        uri = uri,
                        importSessionItemId = importSessionItemId,
                        data = it!!,
                        folderId = folderId
                    )
                }
        }
    }
}