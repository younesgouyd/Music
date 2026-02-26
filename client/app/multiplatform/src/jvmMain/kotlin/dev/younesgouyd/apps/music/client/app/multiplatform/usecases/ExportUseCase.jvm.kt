package dev.younesgouyd.apps.music.client.app.multiplatform.usecases

import kotlinx.serialization.ExperimentalSerializationApi
import java.io.File
import java.net.URI
import java.util.zip.ZipOutputStream
import kotlin.io.copyTo
import kotlin.io.inputStream
import kotlin.io.outputStream
import kotlin.io.path.toPath
import kotlin.use

@OptIn(ExperimentalSerializationApi::class)
actual class ExportUseCaseImpl actual constructor(
    dbDir: File,
    inspectionDir: File,
    mediaDir: File
) : ExportUseCase(
    dbDir,
    inspectionDir,
    mediaDir
) {
    override suspend fun execute(destination: String) {
        val dest = URI(destination)
            .toPath()
            .toFile()
        if (!dest.isDirectory) {
            TODO()
        }
        File(dest, "music.zip").outputStream().use {
            export(it)
        }
    }
}

actual fun File.copyTo(out: ZipOutputStream) {
    return this.inputStream().use { it.copyTo(out) }
}