package dev.younesgouyd.apps.music.server.jvm

import dev.younesgouyd.apps.music.server.common.Application
import java.io.File
import java.util.concurrent.CountDownLatch

fun main() {
    val app = Application()
    val homeDir = File(System.getProperty("user.home"))
    val latch = CountDownLatch(1)
    app.start(homeDir)
    Runtime.getRuntime().addShutdownHook(
        Thread {
            app.stop()
            latch.countDown()
        }
    )
    latch.await()
}