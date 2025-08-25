package dev.younesgouyd.apps.music.common.usecases

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import dev.younesgouyd.apps.music.android.Music
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.data.repoes.MediaFileRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class ImportFolderUseCase actual constructor(
    actual val repoStore: RepoStore,
    actual val saveMp3FileAsTrackUseCase: SaveMp3FileAsTrackUseCase
) {
    private val folderRepo get() = repoStore.folderRepo
    private val mediaFileRepo get() = repoStore.mediaFileRepo

    actual suspend fun execute(uri: String) {
        withContext(Dispatchers.IO) {
            importFolder(uri.toUri())
        }
    }

    private suspend fun importFolder(uri: Uri) {
        fun getFileName(uri: Uri): String = uri.lastPathSegment?.substringAfterLast('/') ?: TODO()
        val context: Context = Music.instance.applicationContext
        fun Uri.isHiddenFolder(): Boolean {
            val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            context.contentResolver.query(this, projection, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val name = cursor.getString(0)
                    return name.startsWith(".")
                }
            }
            return false
        }


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
                        if (!childUri.isHiddenFolder()) {
                            importFolder(childUri, parentId)
                        }
                    } else if (mimeType == "audio/mpeg") {
                        val mediaFileId = mediaFileRepo.add(name = getFileName(childUri), sourceType = MediaFileRepo.SourceType.Local, domainName = null)
                        val internalFile = File(context.filesDir, "media/$mediaFileId")
                        internalFile.createNewFile().also { if (!it) { TODO() } }
                        contentResolver.openInputStream(childUri)?.use { input ->
                            internalFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        saveMp3FileAsTrackUseCase.execute(internalFile, mediaFileId, parentId)
                    }
                }
            }
        }
        val rootId: Long = folderRepo.add("${System.currentTimeMillis()}_imported_from_system_file_picker", null)
        importFolder(uri, rootId)
    }
}
