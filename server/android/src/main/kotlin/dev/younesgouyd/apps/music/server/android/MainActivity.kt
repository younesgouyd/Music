package dev.younesgouyd.apps.music.server.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MusicBackendService.isRunning) {
            Toast.makeText(this, "Music Server is already running", Toast.LENGTH_SHORT).show()
        } else {
            startForegroundService(
                Intent(this, MusicBackendService::class.java)
            )
        }

        finish()
    }
}