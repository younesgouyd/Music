package dev.younesgouyd.apps.music.client.android

import android.app.Application
import android.content.ComponentName
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.younesgouyd.apps.music.client.components.Main
import dev.younesgouyd.apps.music.client.components.SplashScreen
import dev.younesgouyd.apps.music.client.data.RepoStore
import dev.younesgouyd.apps.music.client.data.room.AppDatabase
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class Music : Application() {
    companion object {
        lateinit var instance: Music
            private set
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var currentComponent: MutableStateFlow<Component>

    override fun onCreate() {
        super.onCreate()
        instance = this

        val repoStore = RepoStore(
            appDir = filesDir,
            applicationScope = coroutineScope,
            database = run {
                val appContext = this.applicationContext
                val dbFile = appContext.getDatabasePath("younesmusic.db")
                Room.databaseBuilder<AppDatabase>(context = appContext, name = dbFile.absolutePath)
                    .setDriver(BundledSQLiteDriver())
                    .setQueryCoroutineContext(Dispatchers.IO)
                    .build()
            }
        )

        currentComponent = MutableStateFlow(
            SplashScreen(
                repoStore = repoStore,
                showContent = {
                    val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
                    coroutineScope.launch {
                        currentComponent.update {
                            it.clear()
                            Main(
                                repoStore = repoStore,
                                mediaPlayer = MediaPlayer(
                                    media3Controller = withContext(Dispatchers.IO) {
                                        MediaController.Builder(this@Music, sessionToken)
                                            .buildAsync()
                                            .get()
                                    }
                                )
                            )
                        }
                    }
                }
            )
        )
    }

    @Composable
    fun Ui() {
        val currentComponent by currentComponent.collectAsState()

        currentComponent.show(
            Modifier.fillMaxSize()
                .systemBarsPadding()
        )
    }

    private class MediaPlayer(
        private val media3Controller: MediaController
    ) : dev.younesgouyd.apps.music.client.components.util.MediaController.MediaPlayer() {
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
                        eventListener.onTimePositionChange(newPosition.positionMs.milliseconds)
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

        override fun setTime(time: Duration) {
            media3Controller.seekTo(time.inWholeMilliseconds)
        }

        override fun release() {
            media3Controller.release()
        }
    }
}