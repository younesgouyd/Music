package dev.younesgouyd.apps.music.client

import android.content.ComponentName
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.younesgouyd.apps.music.client.data.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

actual class MusicImpl : Music() {
    val context = MusicAndroidApp.instance

    override val appDir: File = File(context.filesDir, "younesmusicdata")
    override val dbDir: File = File(appDir, "db")
    override val dbFile: File = File(dbDir, "younesmusic.db")

    override suspend fun initDb() {
        println("--> initDb")
        withContext(Dispatchers.IO) {
            if (!dbDir.exists()) {
                println("... initDb | creating dbDir")
                dbDir.mkdir().also { if (!it) TODO() }
            }
            if (!dbFile.exists()) {
                println("... initDb | creating dbFile")
                dbFile.createNewFile().also { if (!it) TODO() }
            }
            println("... initDb | creating db instance")
            db = Room.databaseBuilder<AppDatabase>(context = MusicAndroidApp.instance, name = dbFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
        println("<-- initDb")
    }

    override suspend fun createMediaPlayer() {
        withContext(Dispatchers.IO) {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val media3Controller = MediaController.Builder(context, sessionToken)
                .buildAsync()
                .get()
            mediaPlayer = object : dev.younesgouyd.apps.music.client.components.util.MediaController.MediaPlayer() {
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
    }

    override fun openZipInputStreamFromUri(uri: String): ZipInputStream {
        return ZipInputStream(
            context.contentResolver.openInputStream(uri.toUri())
        )
    }
}