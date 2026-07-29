package dev.younesgouyd.apps.music.server.common.usecases

import androidx.core.net.toUri
import dev.younesgouyd.apps.music.common.applicationContext
import java.io.InputStream

actual fun getFileName(uri: String): String {
    return uri.toUri().lastPathSegment?.substringAfterLast('/') ?: TODO()
}

actual fun getInputStream(uri: String): InputStream {
    return applicationContext
        .contentResolver
        .openInputStream(uri.toUri())!! // TODO
}
