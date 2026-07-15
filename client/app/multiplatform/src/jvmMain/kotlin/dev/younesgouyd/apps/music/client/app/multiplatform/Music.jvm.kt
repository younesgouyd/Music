package dev.younesgouyd.apps.music.client.app.multiplatform

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.AppDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
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
                private var eventListener: EventListener? = null
                private var grabber: FFmpegFrameGrabber? = null
                private var line: SourceDataLine? = null
                private var playbackThread: Thread? = null

                @Volatile private var isPlaying = false
                @Volatile private var isStopped = true
                @Volatile private var seekRequestedTime: Long? = null

                override fun registerEventListener(eventListener: EventListener) {
                    this.eventListener = eventListener
                }

                override fun setMedia(uri: String) {
                    stop()

                    if (Thread.currentThread() != playbackThread) {
                        playbackThread?.interrupt()
                        try {
                            playbackThread?.join(1000)
                        } catch (e: Exception) {}
                    }

                    try { line?.close() } catch (e: Exception) {}
                    line = null

                    try { grabber?.close() } catch (e: Exception) {}
                    grabber = null

                    isStopped = false
                    isPlaying = false

                    try {
                        val g = FFmpegFrameGrabber(
                            if (uri.startsWith("file:")) File(URI(uri)).absolutePath else { uri }
                        )
                        g.start()
                        grabber = g

                        playbackThread = Thread {
                            runPlaybackLoop(g)
                        }.apply {
                            isDaemon = true
                            name = "JavaCV-Playback-Thread"
                            start()
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to initialize JavaCV media grabber" }
                    }
                }

                override fun play() {
                    if (!isPlaying) {
                        isPlaying = true
                        eventListener?.onPlaying()
                    }
                }

                override fun pause() {
                    if (isPlaying) {
                        isPlaying = false
                        eventListener?.onPaused()
                    }
                }

                override fun stop() {
                    isPlaying = false
                    isStopped = true
                    line?.stop()
                    line?.flush()
                    eventListener?.onStopped()
                }

                override fun setTime(time: Duration) {
                    seekRequestedTime = time.inWholeMicroseconds
                }

                override fun release() {
                    stop()
                    playbackThread?.interrupt()
                    try { line?.close() } catch (e: Exception) {}
                    try { grabber?.close() } catch (e: Exception) {}
                }

                private fun runPlaybackLoop(g: FFmpegFrameGrabber) {
                    while (!Thread.currentThread().isInterrupted && !isStopped) {
                        if (!isPlaying) {
                            try { Thread.sleep(20) } catch (e: InterruptedException) { break }
                            continue
                        }

                        val seekTime = seekRequestedTime
                        if (seekTime != null) {
                            seekRequestedTime = null
                            try {
                                g.timestamp = seekTime
                                line?.flush()
                            } catch (e: Exception) {
                                logger.error(e) { "Error seeking" }
                            }
                        }

                        try {
                            val frame = g.grabSamples()
                            if (frame == null) {
                                isPlaying = false
                                isStopped = true
                                eventListener?.onFinished()
                                break
                            }

                            if (frame.samples != null && frame.samples.isNotEmpty()) {
                                eventListener?.onTimePositionChange((g.timestamp / 1000).milliseconds)

                                if (line == null) {
                                    val sampleRate = g.sampleRate
                                    val channels = g.audioChannels
                                    val buf0 = frame.samples[0]

                                    val encoding = when (buf0) {
                                        is FloatBuffer -> AudioFormat.Encoding.PCM_FLOAT
                                        else -> AudioFormat.Encoding.PCM_SIGNED
                                    }
                                    val bitsPerSample = when (buf0) {
                                        is FloatBuffer -> 32
                                        is ShortBuffer -> 16
                                        else -> 8
                                    }
                                    val frameSize = (bitsPerSample / 8) * channels

                                    val audioFormat = AudioFormat(
                                        encoding, sampleRate.toFloat(), bitsPerSample,
                                        channels, frameSize, sampleRate.toFloat(), false
                                    )

                                    val info = DataLine.Info(SourceDataLine::class.java, audioFormat)
                                    val l = AudioSystem.getLine(info) as SourceDataLine
                                    l.open(audioFormat)
                                    l.start()
                                    line = l
                                }

                                val byteArray = interleaveSamples(frame)
                                if (byteArray != null && byteArray.isNotEmpty()) {
                                    line?.write(byteArray, 0, byteArray.size)
                                }
                            }
                        } catch (e: Exception) {
                            break
                        }
                    }
                }

                private fun interleaveSamples(frame: Frame): ByteArray? {
                    val samples = frame.samples ?: return null
                    val channels = samples.size
                    if (channels == 0) return null

                    samples.forEach { it.rewind() }
                    val buf0 = samples[0]

                    return when (buf0) {
                        is ShortBuffer -> {
                            val numSamplesPerChannel = buf0.remaining()
                            val totalSamples = numSamplesPerChannel * channels
                            val shortArray = ShortArray(totalSamples)

                            if (channels == 1) {
                                buf0.get(shortArray)
                            } else {
                                val channelBuffers = Array(channels) { c -> samples[c] as ShortBuffer }
                                var idx = 0
                                for (i in 0 until numSamplesPerChannel) {
                                    for (c in 0 until channels) {
                                        if (channelBuffers[c].hasRemaining()) shortArray[idx++] = channelBuffers[c].get()
                                    }
                                }
                            }

                            val byteArray = ByteArray(totalSamples * 2)
                            ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shortArray)
                            byteArray
                        }
                        is FloatBuffer -> {
                            val numSamplesPerChannel = buf0.remaining()
                            val totalSamples = numSamplesPerChannel * channels
                            val floatArray = FloatArray(totalSamples)

                            if (channels == 1) {
                                buf0.get(floatArray)
                            } else {
                                val channelBuffers = Array(channels) { c -> samples[c] as FloatBuffer }
                                var idx = 0
                                for (i in 0 until numSamplesPerChannel) {
                                    for (c in 0 until channels) {
                                        if (channelBuffers[c].hasRemaining()) floatArray[idx++] = channelBuffers[c].get()
                                    }
                                }
                            }

                            val byteArray = ByteArray(totalSamples * 4)
                            ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().put(floatArray)
                            byteArray
                        }
                        is ByteBuffer -> {
                            val numSamplesPerChannel = buf0.remaining()
                            val totalSamples = numSamplesPerChannel * channels
                            val byteArray = ByteArray(totalSamples)

                            if (channels == 1) {
                                buf0.get(byteArray)
                            } else {
                                val channelBuffers = Array(channels) { c -> samples[c] as ByteBuffer }
                                var idx = 0
                                for (i in 0 until numSamplesPerChannel) {
                                    for (c in 0 until channels) {
                                        if (channelBuffers[c].hasRemaining()) byteArray[idx++] = channelBuffers[c].get()
                                    }
                                }
                            }
                            byteArray
                        }
                        else -> null
                    }
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