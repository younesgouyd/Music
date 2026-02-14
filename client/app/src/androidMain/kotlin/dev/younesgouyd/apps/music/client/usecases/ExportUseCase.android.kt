package dev.younesgouyd.apps.music.client.usecases

import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dev.younesgouyd.apps.music.client.MusicAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
actual class ExportUseCaseImpl actual constructor(
    dbDir: File,
    inspectionDir: File,
    mediaDir: File
) : ExportUseCase(
    dbDir,
    inspectionDir,
    mediaDir
) {
    val context = MusicAndroidApp.instance

    override suspend fun execute(destination: String) {
        withContext(Dispatchers.IO) {
            val destFolder = DocumentFile.fromTreeUri(context, destination.toUri())
                ?: TODO("Invalid destination folder URI: $destination")

            val zipDoc = destFolder.createFile("application/zip", "music.zip")
                ?: TODO("Failed to create music.zip in selected folder")

            context.contentResolver.openOutputStream(zipDoc.uri)?.use {
                export(it)
            } ?: TODO("Unable to open output stream for music.zip")
        }
    }
}
