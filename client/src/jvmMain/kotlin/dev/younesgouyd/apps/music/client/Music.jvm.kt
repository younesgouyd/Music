package dev.younesgouyd.apps.music.client

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.younesgouyd.apps.music.client.components.Main
import dev.younesgouyd.apps.music.client.components.SplashScreen
import dev.younesgouyd.apps.music.client.components.util.MediaController
import dev.younesgouyd.apps.music.client.data.RepoStore
import dev.younesgouyd.apps.music.client.data.room.AppDatabase
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

actual class MusicImpl : Music() {
    override val appDir = File("younesmusicdata").also { it.mkdir() }
    override val repoStore = RepoStore(
        appDir = appDir,
        applicationScope = coroutineScope,
        database = run {
            val file = File("younesmusicdata", "younesmusic.db")
            if (!file.exists()) {
                file.createNewFile()
            }
            Room.databaseBuilder<AppDatabase>(name = file.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
    )
    override val currentComponent: MutableStateFlow<Component> = MutableStateFlow(
        SplashScreen(
            repoStore = repoStore,
            showContent = ::showContent
        )
    )

    override fun showContent() {
        currentComponent.update {
            it.clear()
            Main(repoStore = repoStore, mediaPlayer = MediaPlayer())
        }
    }

    private class MediaPlayer() : MediaController.MediaPlayer() {
        private val vlcPlayer = AudioPlayerComponent().mediaPlayer()

        init {
            NativeDiscovery().discover()
        }

        override fun registerEventListener(eventListener: EventListener) {
            vlcPlayer.events().addMediaPlayerEventListener(
                object : MediaPlayerEventAdapter() {
                    override fun playing(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?) {
                        eventListener.onPlaying()
                    }

                    override fun paused(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?) {
                        eventListener.onPaused()
                    }

                    override fun stopped(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?) {
                        eventListener.onStopped()
                    }

                    override fun timeChanged(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?, newTime: Long) {
                        eventListener.onTimePositionChange(newTime.milliseconds)
                    }

                    override fun finished(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?) {
                        eventListener.onFinished()
                    }
                }
            )
        }

        override fun setMedia(uri: String) {
            stop()
            vlcPlayer.media().startPaused(uri)
        }

        override fun play() {
            vlcPlayer.controls().play()
        }

        override fun pause() {
            vlcPlayer.controls().pause()
        }

        override fun stop() {
            vlcPlayer.controls().stop()
        }

        override fun setTime(time: Duration) {
            vlcPlayer.controls().setTime(time.inWholeMilliseconds)
        }

        override fun release() {
            vlcPlayer.release()
        }
    }
}