package dev.younesgouyd.apps.music.client.app.jvm

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.*
import dev.younesgouyd.apps.music.client.app.multiplatform.MusicImpl

@OptIn(ExperimentalFoundationApi::class)
fun main() {
    val app = MusicImpl()
    application {
        Window(
            state = rememberWindowState(
                placement = WindowPlacement.Maximized,
                position = WindowPosition(Alignment.Center)
            ),
            onCloseRequest = { app.clear(); exitApplication() },
            onPreviewKeyEvent = { event ->
                if (event.type == KeyEventType.KeyUp && event.key == Key.Escape) {
                    app.navigateBack()
                    true
                } else {
                    false
                }
            },
            content = { app.show(Modifier.fillMaxSize()) },
        )
    }
}
