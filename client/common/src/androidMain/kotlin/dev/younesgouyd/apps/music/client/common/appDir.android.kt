package dev.younesgouyd.apps.music.client.common

import dev.younesgouyd.apps.music.common.applicationContext
import java.io.File

actual val appDir: File by lazy { applicationContext.filesDir }