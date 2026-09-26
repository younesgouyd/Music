package dev.younesgouyd.apps.music.client.multiplatform

import dev.younesgouyd.apps.music.common.MediaPlayer
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.playback.MediaPlayerState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import java.io.File
import java.nio.ShortBuffer
import java.util.concurrent.Executors
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

expect fun createMediaPlayer(
    getFile: suspend (MediaFileId) -> File
): MediaPlayer

abstract class LocalMediaPlayer(
    private val getFile: suspend (MediaFileId) -> File
) : MediaPlayer() {
    protected companion object {
        const val BIT_DEPTH: Int = 16
        const val SAMPLE_RATE: Int = 48000
        const val CHANNELS: Int = 2
    }

    private val dispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "JavaCV-Playback-Worker").apply {
            isDaemon = true // TODO
        }
    }.asCoroutineDispatcher()
    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())
    private val mutex = Mutex()
    private val logger = KotlinLogging.logger {}
    protected val _state: MutableStateFlow<MediaPlayerState> = MutableStateFlow(MediaPlayerState.initial())
    protected val commands = Channel<PlayerCommand>(
        capacity = Channel.RENDEZVOUS,
        onUndeliveredElement = { cmd ->
            cmd.ack.completeExceptionally(CancellationException())
        }
    )
    override val state: StateFlow<MediaPlayerState> = _state.asStateFlow()

    protected val FFmpegFrameGrabber?.positionMs get() = (this?.timestamp ?: 0L) / 1000L

    init {
        coroutineScope.launch {
            runPlayerEventLoop()
        }
    }

    override suspend fun setTrack(id: MediaFileId) {
        val file = coroutineScope.async(Dispatchers.IO) { getFile(id).resolved() }
        dispatchCommand { ack -> PlayerCommand.SetTrack(id, file, ack) }
    }

    override suspend fun play() {
        dispatchCommand { ack -> PlayerCommand.Play(ack) }
    }

    override suspend fun pause() {
        dispatchCommand { ack -> PlayerCommand.Pause(ack) }
    }

    override suspend fun stop() {
        dispatchCommand { ack -> PlayerCommand.Stop(ack) }
    }

    override suspend fun setTime(time: Duration) {
        dispatchCommand { ack -> PlayerCommand.Seek(time, ack) }
    }

    override suspend fun setState(value: MediaPlayerState) {
        val file = coroutineScope.async(Dispatchers.IO) { value.track?.let { getFile(it).resolved() } }
        dispatchCommand { ack -> PlayerCommand.SetState(value, file, ack) }
    }

    override suspend fun release() {
        commands.close()
        coroutineScope.cancel()
        dispatcher.close()
    }

    private suspend fun dispatchCommand(
        commandFactory: (CompletableDeferred<Unit>) -> PlayerCommand
    ) {
        if (!mutex.tryLock()) {
            logger.warn { "Player is busy processing another command. Request dropped." }
            return
        }
        val ack = CompletableDeferred<Unit>()
        val cmd = commandFactory(ack)
        try {
            commands.send(cmd)
            ack.await()
        } finally {
            mutex.unlock()
        }
    }

    protected suspend fun CoroutineScope.runPlayerEventLoop() {
        var grabber: FFmpegFrameGrabber? = null
        var line: AudioOutput? = null
        var trackId: MediaFileId? = null
        var isPlaying = false
        var isFinished = false

        fun emitState() {
            _state.value = MediaPlayerState(
                track = trackId,
                isPlaying = isPlaying,
                startPositionMs = grabber.positionMs,
                isFinished = isFinished
            )
        }

        suspend fun setTrack(id: MediaFileId?, uri: Deferred<String?>) {
            line?.apply { stop(); flush(); release() }
            line = null
            grabber?.close()
            grabber = null
            trackId = id
            isPlaying = false
            isFinished = false
            val uri = uri.await()
            if (uri != null) {
                val g = FFmpegFrameGrabber(uri).apply {
                    sampleFormat = avutil.AV_SAMPLE_FMT_S16
                    sampleRate = SAMPLE_RATE
                    audioChannels = CHANNELS
                    start()
                }
                grabber = g
            }
            emitState()
        }

        fun play() {
            if (isFinished) {
                grabber?.timestamp = 0L
                isFinished = false
            }
            isPlaying = true
            line?.play()
        }

        fun seek(position: Duration) {
            grabber?.timestamp = position.inWholeMicroseconds
            line?.flush()
        }

        try {
            while (isActive) {
                val cmd = if (isPlaying) commands.tryReceive().getOrNull() else commands.receive()
                if (cmd != null) {
                    try {
                        when (cmd) {
                            is PlayerCommand.SetTrack -> {
                                setTrack(cmd.id, cmd.uri)
                                cmd.ack.complete(Unit)
                            }
                            is PlayerCommand.Play -> {
                                play()
                                emitState()
                                cmd.ack.complete(Unit)
                            }
                            is PlayerCommand.Pause -> {
                                isPlaying = false
                                line?.stop()
                                emitState()
                                cmd.ack.complete(Unit)
                            }
                            is PlayerCommand.Stop -> {
                                isPlaying = false
                                isFinished = false
                                line?.apply { stop(); flush() }
                                grabber?.timestamp = 0L
                                emitState()
                                cmd.ack.complete(Unit)
                            }
                            is PlayerCommand.Seek -> {
                                seek(cmd.position)
                                isFinished = false
                                emitState()
                                cmd.ack.complete(Unit)
                            }
                            is PlayerCommand.SetState -> {
                                val state = cmd.value
                                setTrack(state.track, cmd.uri)
                                seek(state.startPositionMs.milliseconds)
                                if (state.isPlaying && state.track != null) {
                                    play()
                                }
                                emitState()
                                cmd.ack.complete(Unit)
                            }
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "Error executing command" }
                        cmd.ack.completeExceptionally(e)
                    }
                }

                if (isPlaying && grabber != null) {
                    try {
                        val frame: Frame? = grabber!!.grabSamples() // TODO
                        if (frame == null) {
                            // TODO: output buffer may still hold some data, call SourceDataLine::drain and suspend
                            //  until AudioTrack consume all data while at the same time allow the user to interrupt playback
                            isPlaying = false
                            isFinished = true
                            emitState()
                            continue
                        }
                        val samples: ShortBuffer? = frame.samples?.firstOrNull() as? ShortBuffer
                        if (samples != null && samples.remaining() > 0) {
                            if (line == null) {
                                line = createAudioOutput(frame)
                                line!!.play()
                            }
                            line!!.write(samples)
                        }
                    } catch (e: Throwable) {
                        logger.error(e) { "Playback error" }
                        isPlaying = false
                        emitState()
                    }
                }
            }
        } finally {
            try { line?.apply { stop(); flush(); release() } } catch (_: Throwable) {}
            try { grabber?.close() } catch (_: Throwable) {}
        }
    }

    protected abstract fun createAudioOutput(frame: Frame): AudioOutput

    protected sealed class PlayerCommand {
        abstract val ack: CompletableDeferred<Unit>
        data class SetTrack(val id: MediaFileId, val uri: Deferred<String>, override val ack: CompletableDeferred<Unit>) : PlayerCommand()
        data class Play(override val ack: CompletableDeferred<Unit>) : PlayerCommand()
        data class Pause(override val ack: CompletableDeferred<Unit>) : PlayerCommand()
        data class Stop(override val ack: CompletableDeferred<Unit>) : PlayerCommand()
        data class Seek(val position: Duration, override val ack: CompletableDeferred<Unit>) : PlayerCommand()
        data class SetState(val value: MediaPlayerState, val uri: Deferred<String?>, override val ack: CompletableDeferred<Unit>) : PlayerCommand()
    }

    protected abstract class AudioOutput {
        abstract fun play()
        abstract fun write(samples: ShortBuffer)
        abstract fun stop()
        abstract fun flush()
        abstract fun release()
    }
}