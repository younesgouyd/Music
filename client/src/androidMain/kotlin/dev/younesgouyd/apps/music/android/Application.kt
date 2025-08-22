package dev.younesgouyd.apps.music.android

import android.app.Application

class Music : Application() {
    companion object {
        lateinit var instance: Music
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}