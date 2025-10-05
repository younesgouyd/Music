package dev.younesgouyd.apps.music.common.usecases

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import dev.younesgouyd.apps.music.android.Music
import dev.younesgouyd.apps.music.common.Inspection
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.util.ImportSourceType
import java.io.File

actual class ImportFolderUseCase actual constructor(
    actual val repoStore: RepoStore,
    actual val saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase,
    actual val saveMp3FileAsTrackUseCase: SaveMp3FileAsTrackUseCase
) {
    private val context: Context = Music.instance.applicationContext
    private val mediaDir = File(context.filesDir, "media").also { it.mkdir() }
    private val tempDir = File(context.filesDir, "temp").also { it.mkdir() }
    private val folderRepo get() = repoStore.folderRepo
    private val mediaFileRepo get() = repoStore.mediaFileRepo

    actual suspend fun execute(import: Import): Boolean {
        val rootId: Long = folderRepo.add("${System.currentTimeMillis()}_imported_from_system_file_picker", null)
        importFolder(import, import.folderUri.toUri(), rootId)
        return true // TODO
    }

    private suspend fun importFolder(import: Import, folderUri: Uri, parent: Long?) {
        val parent: Long = folderRepo.add(getFileName(folderUri), parent)
        val contentResolver = context.contentResolver
        val folderDocumentId = when {
            DocumentsContract.isDocumentUri(context, folderUri) -> DocumentsContract.getDocumentId(folderUri)
            else -> DocumentsContract.getTreeDocumentId(folderUri)
        }
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, folderDocumentId)
        contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val documentIdIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val mimeTypeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                val childDocumentId = cursor.getString(documentIdIndex)
                val mimeType = cursor.getString(mimeTypeIndex)
                if (childDocumentId == folderDocumentId) continue
                val childUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, childDocumentId)
                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    if (!childUri.isHiddenFolder()) {
                        importFolder(import, childUri, parent)
                    }
                } else if (mimeType == "audio/mpeg") {
                    val tempFile = File(tempDir, getFileName(childUri))
                    tempFile.createNewFile()
                    contentResolver.openInputStream(childUri)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    val mediaFileId: Long = when (import) {
                        is Import.Local -> importFileLocal(import = import, sourceFile = tempFile, folderId = parent)
                        is Import.Internet -> importFileInternet(import = import, sourceFile = tempFile, folderId = parent)
                    }
                    tempFile.delete()
                    val internalFile = File(mediaDir, mediaFileId.toString())
                    contentResolver.openInputStream(childUri)?.use { input ->
                        internalFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }
    }

    private suspend fun importFileLocal(
        import: Import.Local,
        sourceFile: File,
        folderId: Long
    ): Long {
        val trackId: Long = if (sourceFile.extension.lowercase() == "mp3") {
            saveMp3FileAsTrackUseCase.execute(
                file = sourceFile,
                folderId = folderId
            )
        } else {
            saveAudioFileAsTrackUseCase.execute(
                folderId = folderId,
                title = sourceFile.name, // TODO
                durationSeconds = null,
                artists = emptyList(),
                album = null,
                releaseYear = null,
                albumTrackNumber = null,
                lyrics = null,
                albumImage = null
            )
        }
        return mediaFileRepo.add(
            name = sourceFile.name,
            trackId = trackId,
            sourceUri = import.folderUri,
            sourceWebpageUrl = null,
            sourceType = ImportSourceType.Local
        )
    }

    private suspend fun importFileInternet(
        import: Import.Internet,
        sourceFile: File,
        folderId: Long
    ): Long {
        val inspection: Inspection.Item = import.items.find { it.id == sourceFile.name.toLong() }!!
        val trackId = saveAudioFileAsTrackUseCase.execute(
            folderId = folderId,
            title = inspection.title,
            durationSeconds = inspection.duration?.toLong(),
            artists = inspection.artists,
            album = inspection.album,
            releaseYear = null, // TODO
            albumTrackNumber = null, // TODO
            lyrics = null, // TODO
            albumImage = null // TODO
        )
        return mediaFileRepo.add(
            name = sourceFile.name,
            trackId = trackId,
            sourceUri = import.url,
            sourceWebpageUrl = inspection.url,
            sourceType = ImportSourceType.Internet
        )
    }

    fun getFileName(uri: Uri): String {
        return uri.lastPathSegment?.substringAfterLast('/') ?: TODO()
    }

    private fun Uri.isHiddenFolder(): Boolean {
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        context.contentResolver.query(this, projection, null, null, null).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val name = cursor.getString(0)
                return name.startsWith(".")
            }
        }
        return false
    }
}
