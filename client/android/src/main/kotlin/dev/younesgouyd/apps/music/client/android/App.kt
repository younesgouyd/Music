package dev.younesgouyd.apps.music.client.android

import android.app.Application
import android.net.wifi.WifiManager
import androidx.core.content.getSystemService

class App : Application() {
    lateinit var app: dev.younesgouyd.apps.music.client.multiplatform.Application
        private set

    override fun onCreate() {
        super.onCreate()
        getSystemService<WifiManager>()
            ?.createMulticastLock("App")
            ?.apply { acquire() }
        app = dev.younesgouyd.apps.music.client.multiplatform.ApplicationImpl(this)
    }
}