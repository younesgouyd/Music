package dev.younesgouyd.apps.music.client.common

import java.io.File

actual val appDir: File by lazy {
    File(
        File(System.getProperty("user.home")),
        "younesmusicdata"
    )
}