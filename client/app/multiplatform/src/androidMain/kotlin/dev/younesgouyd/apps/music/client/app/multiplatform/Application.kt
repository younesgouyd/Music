package dev.younesgouyd.apps.music.client.app.multiplatform

import android.app.Application

abstract class MusicAndroidApp : Application() {
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