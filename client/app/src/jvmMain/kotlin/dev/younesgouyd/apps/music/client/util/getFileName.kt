package dev.younesgouyd.apps.music.client.util

import java.net.URI
import kotlin.io.path.toPath

actual fun getFileName(uri: String): String {
    return URI(uri).toPath().toFile().name
}