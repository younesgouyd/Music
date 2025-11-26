package dev.younesgouyd.apps.music.client.usecases

import dev.younesgouyd.apps.music.client.data.RepoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

abstract class ExportUseCase(
    protected val repoStore: RepoStore
) {
    companion object {
        const val VERSION = 1
    }

    abstract suspend fun execute(destination: String)

    suspend fun export(out: OutputStream): Unit = withContext(Dispatchers.IO) {
        ZipOutputStream(out).use { zipOut ->
            zipOut.putNextEntry(ZipEntry("version_$VERSION/"))
            zipOut.closeEntry()

            println("exporting database")
            zipOut.putNextEntry(ZipEntry("db/"))
            zipOut.closeEntry()
            for (file in repoStore.fileManager.dbDir.listFiles().orEmpty()) {
                ensureActive()
                zipOut.putNextEntry(ZipEntry("db/${file.name}"))
                file.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }

            println("exporting inspections")
            zipOut.putNextEntry(ZipEntry("inspections/"))
            zipOut.closeEntry()
            for (file in repoStore.fileManager.inspectionDir.listFiles().orEmpty()) {
                ensureActive()
                zipOut.putNextEntry(ZipEntry("inspections/${file.name}"))
                file.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }

            println("exporting media files")
            zipOut.putNextEntry(ZipEntry("media/"))
            zipOut.closeEntry()
            for (file in repoStore.fileManager.mediaDir.listFiles().orEmpty()) {
                ensureActive()
                zipOut.putNextEntry(ZipEntry("media/${file.name}"))
                file.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }
        }
    }
}

expect class ExportUseCaseImpl(
    repoStore: RepoStore
) : ExportUseCase