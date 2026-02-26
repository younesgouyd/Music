package dev.younesgouyd.apps.music.client.app.multiplatform.util

import java.io.Closeable
import java.io.File
import java.io.InputStream

expect inline fun <T : Closeable?, R> T.use2(block: (T) -> R): R

expect fun getFileName(uri: String): String

expect fun getInputStream(uri: String): InputStream

expect fun InputStream.copyTo(out: File)

expect fun File.writeBytes2(bytes: ByteArray)

expect fun File.writeText2(text: String)
