package dev.younesgouyd.apps.music.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.*
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.younesgouyd.apps.music.common.components.Main
import dev.younesgouyd.apps.music.common.components.SplashScreen
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.data.sqldelight.YounesMusic
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import java.io.File
import java.util.*

object Application {
    private val repoStore: RepoStore
    private val currentComponent: MutableStateFlow<Component>

    init {
        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:younesmusic.db",
            properties = Properties().apply { put("foreign_keys", "true") }
        )
        val file = File("younesmusic.db")
        if (!file.exists()) {
            file.createNewFile()
            YounesMusic.Schema.create(driver)
        }
        repoStore = RepoStore(dbDriver = driver, dataDirectory = "./")
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
