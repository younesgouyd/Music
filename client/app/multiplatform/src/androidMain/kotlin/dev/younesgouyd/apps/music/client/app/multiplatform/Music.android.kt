package dev.younesgouyd.apps.music.client.app.multiplatform

import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.AppDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

actual class MusicImpl : Music() {
    val context = MusicAndroidApp.instance
    private val logger = KotlinLogging.logger {}

    override val appDir: File = File(context.filesDir, "younesmusicdata")
    override val dbDir: File = File(appDir, "db")
    override val dbFile: File = File(dbDir, "younesmusic.db")

    override suspend fun initDb() {
        logger.info { "--> initDb" }
        withContext(Dispatchers.IO) {
            if (!dbDir.exists()) {
                logger.info { "... initDb | creating dbDir" }
                dbDir.mkdir().also { if (!it) TODO() }
            }
            if (!dbFile.exists()) {
                logger.info { "... initDb | creating dbFile" }
                dbFile.createNewFile().also { if (!it) TODO() }
            }
            logger.info { "... initDb | creating db instance" }
            db = Room.databaseBuilder<AppDatabase>(context = MusicAndroidApp.instance, name = dbFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
        logger.info { "<-- initDb" }
    }

    override suspend fun createMediaPlayer() {
        withContext(Dispatchers.IO) {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val media3Controller = MediaController.Builder(context, sessionToken)
                .buildAsync()
                .get()
            val handler = Handler(Looper.getMainLooper())
            mediaPlayer = object : MediaController.MediaPlayer() {
                override fun registerEventListener(eventListener: EventListener) {
                    val updateRunnable = object : Runnable {
                        override fun run() {
                            if (media3Controller.isPlaying) {
                                eventListener.onTimePositionChange(
                                    media3Controller.currentPosition.milliseconds
                                )
                            }
                            handler.postDelayed(this, 250)
                        }
                    }

                    media3Controller.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (isPlaying) {
                                eventListener.onPlaying()
                                handler.post(updateRunnable)
                            } else {
                                eventListener.onPaused()
                                handler.removeCallbacks(updateRunnable)
                            }
                        }

                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_ENDED) {
                                handler.removeCallbacks(updateRunnable)
                                eventListener.onFinished()
                            }
                        }
                    })
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
                    handler.removeCallbacksAndMessages(null)
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