package dev.younesgouyd.apps.music.client.usecases

import java.io.File
import java.net.URI
import kotlin.io.path.toPath

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