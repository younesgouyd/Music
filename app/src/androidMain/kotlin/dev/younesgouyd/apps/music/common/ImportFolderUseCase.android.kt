package dev.younesgouyd.apps.music.common

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import dev.younesgouyd.apps.music.android.Music
import dev.younesgouyd.apps.music.common.data.RepoStore
import java.io.File

actual class ImportFolderUseCase actual constructor(
    actual val repoStore: RepoStore,
    actual val saveMp3FileAsTrackUseCase: SaveMp3FileAsTrackUseCase
) {
    private val folderRepo get() = repoStore.folderRepo

    actual suspend fun execute(uri: String) {
        importFolder(uri.toUri())
    }

    private suspend fun importFolder(uri: Uri) {
        fun getFileName(uri: Uri): String = uri.lastPathSegment?.substringAfterLast('/') ?: TODO()

        val context: Context = Music.instance.applicationContext

        suspend fun importFolder(folderUri: Uri, parent: Long?) {
            val folderName = getFileName(folderUri)
            val parentId: Long = folderRepo.add(folderName, parent)
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
                        importFolder(childUri, parentId)
                    } else if (mimeType == "audio/mpeg") {
                        contentResolver.openInputStream(childUri)?.use { inputStream ->
                            val tempFile = File.createTempFile(getFileName(childUri), ".mp3", context.cacheDir)
                            tempFile.outputStream().use { output -> inputStream.copyTo(output) }
                            try {
                                saveMp3FileAsTrackUseCase.execute(tempFile, childUri.toString(), parentId)
                            } finally {
                                tempFile.delete()
                            }
                        }
                    }
                }
            }
        }

        importFolder(uri, null)
    }
}