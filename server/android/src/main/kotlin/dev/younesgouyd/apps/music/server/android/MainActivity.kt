package dev.younesgouyd.apps.music.server.android

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import dev.younesgouyd.apps.music.server.common.AppState
import dev.younesgouyd.apps.music.server.common.Ui
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

            Ui(
                serverStatus = MusicServerService.appState,
                certificateSignature = MusicServerService.certificateFingerprint.asStateFlow(),
                password = MusicServerService.password.asStateFlow(),
                onStartClick = {
                    if (MusicServerService.appState.value == AppState.Running) {
                        Toast.makeText(this, "Music Server is already running", Toast.LENGTH_SHORT).show()
                    } else {
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