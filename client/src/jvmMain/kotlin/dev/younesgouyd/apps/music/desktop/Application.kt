package dev.younesgouyd.apps.music.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.*
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.younesgouyd.apps.music.common.components.Main
import dev.younesgouyd.apps.music.common.components.SplashScreen
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.data.room.AppDatabase
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import java.io.File

object Application {
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val repoStore: RepoStore
    private val currentComponent: MutableStateFlow<Component>

    init {
        repoStore = RepoStore(
            applicationScope = coroutineScope,
            database = run {
                val file = File("younesmusic.db")
                if (!file.exists()) {
                    file.createNewFile()
                }
                Room.databaseBuilder<AppDatabase>(name = file.absolutePath,)
                    .setDriver(BundledSQLiteDriver())
                    .setQueryCoroutineContext(Dispatchers.IO)
                    .build()
            }
        )
        currentComponent = MutableStateFlow(
            SplashScreen(
                repoStore = repoStore,
                showContent = ::showContent
            )
        )
    }

    fun start() {
        application {
            val currentComponent by currentComponent.collectAsState()

            Window(
                state = rememberWindowState(
                    placement = WindowPlacement.Maximized,
                    position = WindowPosition(Alignment.Center)
                ),
                onCloseRequest = { currentComponent.clear(); exitApplication() },
                content = { currentComponent.show(Modifier.fillMaxSize()) }
            )
        }
    }

    private fun showContent() {
        currentComponent.update {
            it.clear()
            Main(repoStore = repoStore, mediaPlayer = MediaPlayer(), appDir = File("").absolutePath)
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
                        eventListener.onTimePositionChange(newTime)
                    }

                    override fun finished(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?) {
                        eventListener.onFinished()
                    }
                }
            )
        }

        override fun setMedia(uri: String) { stop(); vlcPlayer.media().startPaused(uri) }
        override fun play() { vlcPlayer.controls().play() }
        override fun pause() { vlcPlayer.controls().pause() }
        override fun stop() { vlcPlayer.controls().stop() }
        override fun setTime(time: Long) { vlcPlayer.controls().setTime(time) }
        override fun release() { vlcPlayer.release() }
    }
}
