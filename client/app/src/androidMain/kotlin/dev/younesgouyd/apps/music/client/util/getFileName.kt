package dev.younesgouyd.apps.music.client.util

import androidx.core.net.toUri
import dev.younesgouyd.apps.music.client.getFileName

actual fun getFileName(uri: String): String {
    return getFileName(uri.toUri())
}