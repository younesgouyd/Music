package dev.younesgouyd.apps.music.server.jvm

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.*
import dev.younesgouyd.apps.music.server.multiplatform.Application
import dev.younesgouyd.apps.music.server.multiplatform.ApplicationImpl

fun main() {
    val app: Application = ApplicationImpl()
    application {
        Window(
            state = rememberWindowState(
                placement = WindowPlacement.Maximized,
                position = WindowPosition(Alignment.Center)
            ),
            onCloseRequest = { exitApplication() },
            content = { app.show(Modifier.fillMaxSize()) }
        )
    }
}
