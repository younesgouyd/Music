package dev.younesgouyd.apps.music.client.app.multiplatform

import dev.younesgouyd.apps.music.common.Inspection
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import kotlin.io.path.toPath

private val logger = KotlinLogging.logger {}

actual suspend fun scanFolder(uri: FileUri): List<Inspection.ItemInspection.LocalFileTrack> {
    logger.info { "--> ::scanFolder(uri: $uri)" }
    val folder = URI(uri).toPath().toFile()
    return scanFolder(folder, emptyList())
}

private suspend fun scanFolder(file: File, path: List<String>): List<Inspection.ItemInspection.LocalFileTrack> {
    val result = mutableListOf<Inspection.ItemInspection.LocalFileTrack>()
    withContext(Dispatchers.IO) {
        for (file in file.listFiles()!!) {
            if (file.isDirectory) {
                if (!file.isHidden) {
                    result.addAll(scanFolder(file, path + listOf(file.name)))
                }
            } else if (file.isAudioFile()) {
                if (file.extension.lowercase() != "mp3") { // TODO
                    logger.info { "::scanMetadata | this file is not mp3 and will be skipped: ${file.absolutePath}" }
                } else {
                    result.add(scanMetadata(file, file.toURI().toString(), path))
                }
            }
        }
    }
    return result
}