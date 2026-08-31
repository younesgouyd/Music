package dev.younesgouyd.apps.music.client.multiplatform

import dev.younesgouyd.apps.music.common.applicationContext
import java.io.File

actual val appDir: File by lazy {
    File(
        applicationContext.filesDir,
        "younesmusicdata"
    )
}