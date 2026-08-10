package dev.younesgouyd.apps.music.server.android

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.younesgouyd.apps.music.server.common.Application

class MusicBackendService : Service() {
    private lateinit var app: Application

    override fun onCreate() {
        super.onCreate()
        app = Application()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        app.start(applicationContext.filesDir)
        isRunning = true
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy() // TODO
        app.stop()
        isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "music_server_channel"
        val channel = NotificationChannel(
            channelId, "Music Server Running", NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)

        val stopIntent = Intent(this, MusicBackendService::class.java).apply {
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

        var isRunning = false
            private set
    }
}