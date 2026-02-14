package dev.younesgouyd.apps.music.client.util

import androidx.core.net.toUri
import dev.younesgouyd.apps.music.client.MusicAndroidApp
import java.io.InputStream

actual fun getInputStream(uri: String): InputStream {
    return MusicAndroidApp.instance
        .applicationContext
        .contentResolver
        .openInputStream(uri.toUri())!! // TODO
}