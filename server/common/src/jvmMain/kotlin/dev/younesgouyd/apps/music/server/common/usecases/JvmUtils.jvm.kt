package dev.younesgouyd.apps.music.server.common.usecases

import java.io.InputStream
import java.net.URI
import kotlin.io.path.toPath

actual fun getFileName(uri: String): String {
    return URI(uri).toPath().toFile().name
}

actual fun getInputStream(uri: String): InputStream {
    return URI(uri)
        .toPath()
        .toFile()
        .inputStream()
}