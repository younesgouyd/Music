package dev.younesgouyd.apps.music.common.usecases

import dev.younesgouyd.apps.music.common.Inspection
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.util.ImportSourceType
import java.io.File
import java.net.URI
import kotlin.io.path.toPath
import kotlin.time.Duration.Companion.seconds

actual class ImportFolderUseCase actual constructor(
    actual val repoStore: RepoStore,
    actual val saveAudioFileAsTrackUseCase: SaveAudioFileAsTrackUseCase,
    actual val saveMp3FileAsTrackUseCase: SaveMp3FileAsTrackUseCase
) {
    private val mediaDir = File("media").also { it.mkdir() }
    private val folderRepo get() = repoStore.folderRepo
    private val mediaFileRepo get() = repoStore.mediaFileRepo

    actual suspend fun execute(import: Import): Boolean {
        val sourceType = when (import) {
            is Import.Local -> "system_file_picker"
            is Import.Internet -> "internet"
        }
        val rootId: Long = folderRepo.add("${System.currentTimeMillis()}_imported_from_$sourceType", null)
        importFolder(import, URI(import.folderUri).toPath().toFile(), rootId)
        return true // TODO
    }

    private suspend fun importFolder(import: Import, folder: File, parent: Long?) {
        val parent: Long = folderRepo.add(folder.name, parent)
        for (file in folder.listFiles()!!) {
            if (file.isDirectory) {
                if (!file.isHidden) {
                    importFolder(import, file, parent)
                }
            } else if (file.isAudioFile()) {
                val mediaFileId: Long = when (import) {
                    is Import.Local -> importFileLocal(import = import, sourceFile = file, folderId = parent)
                    is Import.Internet -> importFileInternet(import = import, sourceFile = file, folderId = parent)
                }
                val internalFile = File(mediaDir, mediaFileId.toString())
                file.copyTo(internalFile)
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
                duration = null,
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
            duration = inspection.duration?.seconds,
            artists = inspection.artists,
            album = inspection.album,
            releaseYear = null, // TODO
            albumTrackNumber = null, // TODO
            lyrics = null, // TODO
            albumImage = null
        )
        return mediaFileRepo.add(
            name = sourceFile.name,
            trackId = trackId,
            sourceUri = import.url,
            sourceWebpageUrl = inspection.url,
            sourceType = ImportSourceType.Internet
        )
    }
}
