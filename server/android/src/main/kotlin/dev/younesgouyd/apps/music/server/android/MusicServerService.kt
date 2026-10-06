package dev.younesgouyd.apps.music.server.android

import android.app.*
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import dev.younesgouyd.apps.music.common.ServerState
import dev.younesgouyd.apps.music.server.multiplatform.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MusicServerService : Service() {
    private lateinit var app: Application
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        applicationContext.getSystemService<WifiManager>()
            ?.createMulticastLock("MusicServerService")
            ?.apply { acquire() }
        app = Application()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, createNotification())
        if (serverState.value == ServerState.Stopping) { TODO() }
        if (serverState.value != ServerState.Starting && serverState.value != ServerState.Running) {
            serviceScope.launch {
                app.start(applicationContext.filesDir, MainActivity.ytDlpServerInfo.value)
            }
            serviceScope.launch {
                app.state.collect(serverState)
            }
            serviceScope.launch {
                app.certificateFingerprint.collect(certificateFingerprint)
            }
            serviceScope.launch {
                app.password.collect(password)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.launch {
            app.stop()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "music_server_channel"
        val channel = NotificationChannel(
            channelId, "Music Server Running", NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)

        val stopIntent = Intent(this, MusicServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Music Server")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 101
        private const val ACTION_STOP = "dev.younesgouyd.apps.music.server.android.ACTION_STOP"

        val serverState = MutableStateFlow(ServerState.Stopped)
        val certificateFingerprint = MutableStateFlow("")
        val password = MutableStateFlow("")
    }
}