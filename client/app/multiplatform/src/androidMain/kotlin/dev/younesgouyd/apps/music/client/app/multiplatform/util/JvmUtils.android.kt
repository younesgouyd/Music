package dev.younesgouyd.apps.music.client.app.multiplatform.util

import androidx.core.net.toUri
import dev.younesgouyd.apps.music.client.app.multiplatform.MusicAndroidApp
import dev.younesgouyd.apps.music.client.app.multiplatform.getFileName
import java.io.InputStream

actual fun getFileName(uri: String): String {
    return getFileName(uri.toUri())
}

actual fun getInputStream(uri: String): InputStream {
    return MusicAndroidApp.instance
        .applicationContext
        .contentResolver
        .openInputStream(uri.toUri())!! // TODO
}
