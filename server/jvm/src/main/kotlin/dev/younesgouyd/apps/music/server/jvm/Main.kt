package dev.younesgouyd.apps.music.server.jvm

import androidx.compose.ui.Alignment
import androidx.compose.ui.window.*
import dev.younesgouyd.apps.music.server.multiplatform.Application
import dev.younesgouyd.apps.music.server.multiplatform.Ui
import dev.younesgouyd.apps.music.server.multiplatform.YtDlpServerInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

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
                    certificateFingerprint = MusicServerService.certificateFingerprint,
                    password = MusicServerService.password,
                    onStartClick = {
                        MusicServerService.start(it)
                    },
                    onStopClick = {
                        MusicServerService.stop()
                    }
                )
            }
        )
    }
}

private object MusicServerService {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val app = Application()
    val appState = app.state
    val certificateFingerprint = app.certificateFingerprint
    val password = app.password

    fun start(ytDlpServerInfo: YtDlpServerInfo) {
        scope.launch {
            val homeDir = File(System.getProperty("user.home"))
            app.start(homeDir, ytDlpServerInfo)
        }
    }

    fun stop() {
        scope.launch {
            app.stop()
        }
    }
}