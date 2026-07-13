package dev.younesgouyd.apps.music.client.app.multiplatform.usecases

import kotlinx.serialization.ExperimentalSerializationApi
import java.io.File
import java.net.URI
import kotlin.io.buffered
import kotlin.io.outputStream
import kotlin.io.path.toPath
import kotlin.use

@OptIn(ExperimentalSerializationApi::class)
actual class ExportUseCaseImpl actual constructor(
    dbDir: File,
    mediaDir: File
) : ExportUseCase(
    dbDir,
    mediaDir
) {
    override suspend fun execute(destination: String) {
        val dest = URI(destination).toPath().toFile()
        if (!dest.isDirectory) TODO()

        val targetFile = File(dest, "music.zip")
        try {
            targetFile.outputStream().buffered().use { export(it) }
        } catch (e: Exception) {
            if (targetFile.exists()) {
                targetFile.delete() // Clean up the corrupted file
            }
            throw e
        }

//        val dest = URI(destination)
//            .toPath()
//            .toFile()
//        if (!dest.isDirectory) {
//            TODO()
//        }
//        File(dest, "music.zip").outputStream().use {
//            export(it)
//        }
    }
}