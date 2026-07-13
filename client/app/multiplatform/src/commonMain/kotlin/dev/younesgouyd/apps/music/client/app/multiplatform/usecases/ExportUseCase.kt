package dev.younesgouyd.apps.music.client.app.multiplatform.usecases

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.copyTo
import kotlin.io.inputStream
import kotlin.use

abstract class ExportUseCase(
    private val dbDir: File,
    private val mediaDir: File
) {
    private val logger = KotlinLogging.logger {}
    companion object {
        const val VERSION = 1
    }

    abstract suspend fun execute(destination: String)

    suspend fun export(out: OutputStream): Unit = withContext(Dispatchers.IO) {
        ZipOutputStream(out).use { zipOut ->
            zipOut.putNextEntry(ZipEntry("version_$VERSION/"))
            zipOut.closeEntry()
            logger.info { "exporting database" }
            zipOut.putNextEntry(ZipEntry("db/"))
            zipOut.closeEntry()
            for (file in dbDir.listFiles().orEmpty()) {
                ensureActive()
                zipOut.putNextEntry(ZipEntry("db/${file.name}"))
                file.copyTo(zipOut)
                zipOut.closeEntry()
            }

            logger.info { "exporting media files" }
            zipOut.putNextEntry(ZipEntry("media/"))
            zipOut.closeEntry()
            for (file in mediaDir.listFiles().orEmpty()) {
                ensureActive()
                zipOut.putNextEntry(ZipEntry("media/${file.name}"))
                file.copyTo(zipOut)
                zipOut.closeEntry()
            }
        }
        logger.info { "done" }
    }

    private fun File.copyTo(out: ZipOutputStream) {
        return this.inputStream().use { it.copyTo(out) }
    }
}

expect class ExportUseCaseImpl(
    dbDir: File,
    mediaDir: File
) : ExportUseCase