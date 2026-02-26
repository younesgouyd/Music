package dev.younesgouyd.apps.music.client.app.multiplatform.util

import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.net.URI
import kotlin.io.copyTo
import kotlin.io.inputStream
import kotlin.io.outputStream
import kotlin.io.path.toPath
import kotlin.io.writeBytes
import kotlin.io.writeText
import kotlin.use

actual inline fun <T : Closeable?, R> T.use2(block: (T) -> R): R {
    return this.use(block)
}

actual fun getFileName(uri: String): String {
    return URI(uri).toPath().toFile().name
}

actual fun getInputStream(uri: String): InputStream {
    return URI(uri)
        .toPath()
        .toFile()
        .inputStream()
}

actual fun InputStream.copyTo(out: File) {
    out.outputStream()
        .use { this.copyTo(it) }
}

actual fun File.writeBytes2(bytes: ByteArray) {
    this.writeBytes(bytes)
}

actual fun File.writeText2(text: String) {
    this.writeText(text)
}