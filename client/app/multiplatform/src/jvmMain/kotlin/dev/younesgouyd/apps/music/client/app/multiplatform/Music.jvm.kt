package dev.younesgouyd.apps.music.client.app.multiplatform

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.AppDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import java.io.File
import java.net.URI
import java.util.zip.ZipInputStream
import kotlin.io.path.toPath
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

actual class MusicImpl : Music() {
    override val appDir: File = File(System.getProperty("user.home"), "younesmusicdata")
    override val dbDir: File = File(appDir, "db")
    override val dbFile: File = File(dbDir, "younesmusic.db")
    private val logger = KotlinLogging.logger {}

    init {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            LoggerFactory.getLogger("UncaughtException").error("Crash in ${thread.name}", throwable)
        }
    }

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
            db = Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
        logger.info { "<-- initDb" }
    }

    override suspend fun createMediaPlayer() {
        withContext(Dispatchers.IO) {
            mediaPlayer = object : MediaController.MediaPlayer() {
                private val componentHardReference: AudioPlayerComponent // https://capricasoftware.co.uk/tutorials/vlcj/4/garbage-collection
                private val vlcPlayer: MediaPlayer

                init {
                    NativeDiscovery().discover()
                    componentHardReference = AudioPlayerComponent()
                    vlcPlayer = componentHardReference.mediaPlayer()
                }

                override fun registerEventListener(eventListener: EventListener) {
                    vlcPlayer.events().addMediaPlayerEventListener(
                        object : MediaPlayerEventAdapter() {
                            override fun playing(mediaPlayer: MediaPlayer?) {
                                eventListener.onPlaying()
                            }

                            override fun paused(mediaPlayer: MediaPlayer?) {
                                eventListener.onPaused()
                            }

                            override fun stopped(mediaPlayer: MediaPlayer?) {
                                eventListener.onStopped()
                            }

                            override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) {
                                eventListener.onTimePositionChange(newTime.milliseconds)
                            }

                            override fun finished(mediaPlayer: MediaPlayer?) {
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
                    componentHardReference.release()
                }
            }
        }
    }

    override fun openZipInputStreamFromUri(uri: String): ZipInputStream {
        val file = if (uri.startsWith("file:")) {
            URI(uri).toPath().toFile()
        } else {
            File(uri)
        }
        return ZipInputStream(file.inputStream().buffered())
    }
}