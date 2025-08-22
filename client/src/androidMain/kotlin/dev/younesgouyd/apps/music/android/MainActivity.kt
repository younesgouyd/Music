package dev.younesgouyd.apps.music.android

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.younesgouyd.apps.music.common.components.Main
import dev.younesgouyd.apps.music.common.components.SplashScreen
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.data.sqldelight.YounesMusic
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var _repoStore: RepoStore
    val repoStore by lazy { _repoStore }
    private lateinit var currentComponent: MutableStateFlow<Component>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val driver = AndroidSqliteDriver(schema = YounesMusic.Schema, context = this, "younesmusic.db")
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        _repoStore = RepoStore(dbDriver = driver, dataDirectory = this.filesDir.path)
        currentComponent = MutableStateFlow(
            SplashScreen(repoStore = repoStore, showContent = ::showContent)
        )

        setContent {
            val currentComponent by currentComponent.collectAsState()
            currentComponent.show(
                Modifier.fillMaxSize()
                    .systemBarsPadding()
            )
        }
    }

    private fun showContent() {
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))

        lifecycleScope.launch {
            currentComponent.update {
                it.clear()
                Main(
                    repoStore = repoStore,
                    mediaPlayer = MediaPlayer(
                        media3Controller = withContext(Dispatchers.IO) {
                            MediaController.Builder(this@MainActivity, sessionToken)
                                .buildAsync()
                                .get()
                        }
                    )
                )
            }
        }
    }

    private class MediaPlayer(
        private val media3Controller: MediaController
    ) : dev.younesgouyd.apps.music.common.components.util.MediaController.MediaPlayer() {
        override fun registerEventListener(eventListener: EventListener) {
            media3Controller.addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        eventListener.onPlaying()
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        eventListener.onTimePositionChange(newPosition.positionMs)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            eventListener.onFinished()
                        }
                    }
                }
            )
        }

        override fun setMedia(uri: String) {
            media3Controller.setMediaItem(MediaItem.fromUri(uri))
            media3Controller.prepare()
        }

        override fun play() {
            media3Controller.play()
        }

        override fun pause() {
            media3Controller.pause()
        }

        override fun stop() {
            media3Controller.stop()
        }

        override fun setTime(time: Long) {
            media3Controller.seekTo(time)
        }

        override fun release() {
            media3Controller.release()
        }
    }
}