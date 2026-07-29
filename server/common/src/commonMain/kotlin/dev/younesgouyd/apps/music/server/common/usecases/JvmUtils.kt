package dev.younesgouyd.apps.music.server.common.usecases

import java.io.InputStream

expect fun getFileName(uri: String): String

expect fun getInputStream(uri: String): InputStream
