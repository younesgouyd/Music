package dev.younesgouyd.apps.music.client

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.younesgouyd.apps.music.client.components.util.MediaController
import dev.younesgouyd.apps.music.client.data.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.util.zip.ZipInputStream
import kotlin.io.path.toPath
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

actual class MusicImpl : Music() {
    override val appDir: File = File("younesmusicdata")
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
            db = Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
        println("<-- initDb")
    }

    override suspend fun createMediaPlayer() {
        withContext(Dispatchers.IO) {
            mediaPlayer = object : MediaController.MediaPlayer() {
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
    }

    override fun openZipInputStreamFromUri(uri: String): ZipInputStream {
        return ZipInputStream(
            FileInputStream(
                URI(uri).toPath().toFile()
            )
        )
    }
}