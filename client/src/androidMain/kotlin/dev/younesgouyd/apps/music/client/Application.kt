package dev.younesgouyd.apps.music.client

import android.app.Application

class MusicAndroidApp : Application() {
    companion object {
        lateinit var instance: MusicAndroidApp
            private set
        lateinit var music: MusicImpl
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        music = MusicImpl()
    }
}