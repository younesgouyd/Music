package dev.younesgouyd.apps.music.server.android

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MusicServerService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 101
        private const val ACTION_STOP = "dev.younesgouyd.apps.music.server.android.ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            android.os.Process.killProcess(android.os.Process.myPid())
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    override fun onDestroy() {
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

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Music Server")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                PendingIntent.getService(
                    /* context = */ this,
                    /* requestCode = */ 0,
                    /* intent = */ Intent(this, MusicServerService::class.java).apply { action = ACTION_STOP },
                    /* flags = */ PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()
    }
}