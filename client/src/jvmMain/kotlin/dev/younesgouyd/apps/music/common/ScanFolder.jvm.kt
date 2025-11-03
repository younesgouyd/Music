package dev.younesgouyd.apps.music.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import kotlin.io.path.toPath

actual suspend fun scanFolder(uri: FileUri): List<Inspection.ItemInspection.LocalFileTrack> {
    val folder = URI(uri).toPath().toFile()
    return scanFolder(folder)
}

private suspend fun scanFolder(file: File): List<Inspection.ItemInspection.LocalFileTrack> {
    val result = mutableListOf<Inspection.ItemInspection.LocalFileTrack>()
    withContext(Dispatchers.IO) {
        for (file in file.listFiles()!!) {
            if (file.isDirectory) {
                if (!file.isHidden) {
                    result.addAll(scanFolder(file))
                }
            } else if (file.isAudioFile()) {
                if (file.extension.lowercase() != "mp3") { // TODO
                    println("::scanMetadata | this file is not mp3 and will be skipped: ${file.absolutePath}")
                } else {
                    result.add(scanMetadata(file, file.toURI().toString()))
                }
            }
        }
    }
    return result
}