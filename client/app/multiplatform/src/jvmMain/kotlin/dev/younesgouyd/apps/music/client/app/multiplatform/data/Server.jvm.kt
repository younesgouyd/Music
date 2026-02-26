package dev.younesgouyd.apps.music.client.app.multiplatform.data

import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import java.io.InputStream

actual fun ByteReadChannel.toInputStream2(): InputStream {
    return this.toInputStream()
}