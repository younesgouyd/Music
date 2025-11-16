package dev.younesgouyd.apps.music.client

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.*

fun main() {
    val app = MusicImpl()
    application {
        Window(
            state = rememberWindowState(
                placement = WindowPlacement.Maximized,
                position = WindowPosition(Alignment.Center)
            ),
            onCloseRequest = { app.clear(); exitApplication() },
            content = { app.show(Modifier.fillMaxSize()) }
        )
    }
}
