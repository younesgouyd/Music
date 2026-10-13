package dev.younesgouyd.apps.music.client.jvm

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.*
import dev.younesgouyd.apps.music.client.multiplatform.Application
import dev.younesgouyd.apps.music.client.multiplatform.ApplicationImpl

fun main() {
//    System.setProperty("sun.java2d.uiScale", "1.0")
    val app: Application = ApplicationImpl()
    application {
        Window(
            state = rememberWindowState(
                placement = WindowPlacement.Maximized,
                position = WindowPosition(Alignment.Center)
            ),
            onCloseRequest = { exitApplication() },
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
