package dev.younesgouyd.apps.music.ytdlp

import androidx.compose.ui.Alignment
import androidx.compose.ui.window.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

fun main() {
    application {
        Window(
            state = rememberWindowState(
                placement = WindowPlacement.Maximized,
                position = WindowPosition(Alignment.Center)
            ),
            onCloseRequest = { MusicServerService.stop(); exitApplication() },
            content = {
                Ui(
                    serverStatus = MusicServerService.appState,
                    certificateSignature = MusicServerService.certificateFingerprint,
                    password = MusicServerService.password,
                    onStartClick = {
                        MusicServerService.start()
                    },
                    onStopClick = {
                        MusicServerService.stop()
                    }
                )
            },
        )
    }
}

private object MusicServerService {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val app = Application()
    val appState = app.state
    val certificateFingerprint = app.certificateFingerprint
    val password = app.password

    fun start() {
        scope.launch {
            app.start()
        }
    }

    fun stop() {
        scope.launch {
            app.stop()
        }
    }
}