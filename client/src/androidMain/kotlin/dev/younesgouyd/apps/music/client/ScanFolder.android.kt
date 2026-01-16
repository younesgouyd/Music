package dev.younesgouyd.apps.music.client

import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual suspend fun scanFolder(uri: FileUri): List<Inspection.ItemInspection.LocalFileTrack> {
    val folder = uri.toUri()
    return scanFolder(folder, emptyList())
}

private suspend fun scanFolder(folderUri: Uri, path: List<String>): List<Inspection.ItemInspection.LocalFileTrack> {
    val context = MusicAndroidApp.instance // TODO
    val result = mutableListOf<Inspection.ItemInspection.LocalFileTrack>()
    withContext(Dispatchers.IO) {
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
                        result.addAll(scanFolder(childUri, path + listOf(getFileName(childUri))))
                    }
                } else if (mimeType == "audio/mpeg") {
                    val extension = getFileName(childUri).substringAfterLast(".").lowercase()
                    if (extension != "mp3") { // TODO
                        println("::scanMetadata | this file is not mp3 and will be skipped: $childUri")
                    } else {
                        val fileNameWithoutExtension = getFileName(childUri).substringBeforeLast(".")
                        val tempFile = File.createTempFile(
                            fileNameWithoutExtension,
                            extension,
                            context.cacheDir
                        ) // TODO
                        contentResolver.openInputStream(childUri)!!.use { input ->
                            tempFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        result.add(scanMetadata(tempFile, childUri.toString(), path))
                        tempFile.delete()
                    }
                }
            }
        }
    }
    return result
}

private fun Uri.isHiddenFolder(): Boolean {
    val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
    val context = MusicAndroidApp.instance // TODO
    context.contentResolver.query(this, projection, null, null, null).use { cursor ->
        if (cursor != null && cursor.moveToFirst()) {
            val name = cursor.getString(0)
            return name.startsWith(".")
        }
    }
    return false
}

fun getFileName(uri: Uri): String {
    return uri.lastPathSegment?.substringAfterLast('/') ?: TODO()
}