package dev.younesgouyd.apps.music.client.util

import java.io.InputStream
import java.net.URI
import kotlin.io.path.toPath

actual fun getInputStream(uri: String): InputStream {
    return URI(uri)
        .toPath()
        .toFile()
        .inputStream()
}