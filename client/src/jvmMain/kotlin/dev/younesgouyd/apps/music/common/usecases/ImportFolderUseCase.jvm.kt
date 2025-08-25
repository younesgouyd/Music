package dev.younesgouyd.apps.music.common.usecases

import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.data.repoes.MediaFileRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI

actual class ImportFolderUseCase actual constructor(
    actual val repoStore: RepoStore,
    actual val saveMp3FileAsTrackUseCase: SaveMp3FileAsTrackUseCase
) {
    private val folderRepo get() = repoStore.folderRepo
    private val mediaFileRepo get() = repoStore.mediaFileRepo

    actual suspend fun execute(uri: String) {
        withContext(Dispatchers.IO) {
            val file = File(URI.create(uri))
            if (!file.isDirectory) {
                TODO()
            }
            importFolder(file)
        }
    }

    private suspend fun importFolder(folder: File) {
        suspend fun importFolder(folder: File, parent: Long?) {
            val parent: Long = folderRepo.add(folder.name, parent)
            for (file in folder.listFiles()!!) {
                if (file.isDirectory) {
                    if (!file.isHidden) {
                        importFolder(file, parent)
                    }
                } else if (file.extension.lowercase() == "mp3") {
                    val mediaFileId = mediaFileRepo.add(name = file.name, sourceType = MediaFileRepo.SourceType.Local, domainName = null)
                    val internalFile = File("./media/$mediaFileId")
                    file.copyTo(internalFile)
                    saveMp3FileAsTrackUseCase.execute(internalFile, mediaFileId, parent)
                }
            }
        }
        val rootId: Long = folderRepo.add("${System.currentTimeMillis()}_imported_from_system_file_picker", null)
        importFolder(folder, rootId)
    }
}