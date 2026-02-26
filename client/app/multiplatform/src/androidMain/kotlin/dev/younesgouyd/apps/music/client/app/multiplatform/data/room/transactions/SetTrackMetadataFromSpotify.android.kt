package dev.younesgouyd.apps.music.client.app.multiplatform.data.room.transactions

import java.net.URL

actual fun URL.readBytes2(): ByteArray {
    return this.readBytes()
}