package dev.younesgouyd.apps.music.client

import android.content.ComponentName
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

actual class MusicImpl : Music() {
    override val appDir: File = MusicAndroidApp.instance.filesDir
    override val repoStore = RepoStore(
        appDir = appDir,
        applicationScope = coroutineScope,
        database = run {
            val appContext = MusicAndroidApp.instance
            val dbFile = appContext.getDatabasePath("younesmusic.db")
            Room.databaseBuilder<AppDatabase>(context = appContext, name = dbFile.absolutePath)
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
        val sessionToken = SessionToken(MusicAndroidApp.instance, ComponentName(MusicAndroidApp.instance, PlaybackService::class.java))
        coroutineScope.launch {
            currentComponent.update {
                it.clear()
                Main(
                    repoStore = repoStore,
                    mediaPlayer = MediaPlayer(
                        media3Controller = withContext(Dispatchers.IO) {
                            MediaController.Builder(MusicAndroidApp.instance, sessionToken)
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