package dev.younesgouyd.apps.music.client.app.multiplatform.util

import java.io.InputStream

expect fun getFileName(uri: String): String

expect fun getInputStream(uri: String): InputStream
