package dev.younesgouyd.apps.music.server.android

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.common.ServerState
import dev.younesgouyd.apps.music.server.multiplatform.Ui
import dev.younesgouyd.apps.music.server.multiplatform.YtDlpServerInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    startForegroundService(Intent(this, MusicServerService::class.java))
                } else {
                    Toast.makeText(
                        this,
                        "Notification permission is required",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            Box(
                modifier = Modifier.fillMaxSize().systemBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Ui(
                    serverStatus = MusicServerService.serverState,
                    certificateFingerprint = MusicServerService.certificateFingerprint.asStateFlow(),
                    password = MusicServerService.password.asStateFlow(),
                    onStartClick = {
                        if (MusicServerService.serverState.value == ServerState.Running) {
                            Toast.makeText(this@MainActivity, "Music Server is already running", Toast.LENGTH_SHORT).show()
                        } else {
                            ytDlpServerInfo.value = it
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onStopClick = {
                        stopService(Intent(this@MainActivity, MusicServerService::class.java))
                    }
                )
            }
        }
    }

    companion object {
        val ytDlpServerInfo = MutableStateFlow(YtDlpServerInfo.Empty)
    }
}