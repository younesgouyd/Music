package dev.younesgouyd.apps.music.common

import dev.younesgouyd.apps.music.common.data.RepoStore
import java.io.File
import java.net.URI

actual class ImportFolderUseCase actual constructor(
    actual val repoStore: RepoStore,
    actual val saveMp3FileAsTrackUseCase: SaveMp3FileAsTrackUseCase
) {
    private val folderRepo get() = repoStore.folderRepo

    actual suspend fun execute(uri: String) {
        val file = File(URI.create(uri))
        if (!file.isDirectory) { TODO() }
        importFolder(file)
    }

    private suspend fun importFolder(folder: File) {
        fun File.toCorrectFileUri(): String = this.toPath().toUri().toString().replace("file:/", "file:///")
        suspend fun importFolder(folder: File, parent: Long?) {
            val parent: Long = folderRepo.add(folder.name, parent)
            for (file in folder.listFiles()!!) {
                if (file.isDirectory) {
                    importFolder(file, parent)
                } else if (file.extension.lowercase() == "mp3") {
                    saveMp3FileAsTrackUseCase.execute(file, file.toCorrectFileUri(), parent)
                }
            }
        }
        importFolder(folder, null)
    }
}