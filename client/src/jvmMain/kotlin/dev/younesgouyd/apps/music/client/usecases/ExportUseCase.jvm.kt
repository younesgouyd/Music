package dev.younesgouyd.apps.music.client.usecases

import dev.younesgouyd.apps.music.client.data.RepoStore
import java.io.File
import java.net.URI
import kotlin.io.path.toPath

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
actual class ExportUseCaseImpl actual constructor(
    repoStore: RepoStore
) : ExportUseCase(repoStore) {
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