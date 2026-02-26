package dev.younesgouyd.apps.music.client.app.multiplatform.components.util

import java.io.File
import java.net.URL

actual fun File.readBytes2(): ByteArray {
    return this.readBytes()
}

actual fun URL.readBytes2(): ByteArray {
    return this.readBytes()
}