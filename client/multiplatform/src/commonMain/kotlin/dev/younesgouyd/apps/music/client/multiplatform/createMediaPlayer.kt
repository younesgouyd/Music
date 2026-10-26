package dev.younesgouyd.apps.music.client.multiplatform

import dev.younesgouyd.apps.music.common.MediaPlayer
import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.datamodels.MediaPlayerState
import dev.younesgouyd.apps.music.common.models.datamodels.PlaybackContext
import dev.younesgouyd.apps.music.common.models.datamodels.PlaybackInterval
import dev.younesgouyd.apps.music.common.models.datamodels.toClientSent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import java.io.File
import java.nio.ShortBuffer
import java.util.concurrent.Executors
import kotlin.coroutines.cancellation.CancellationException

expect fun createMediaPlayer(
    clientId: ClientId,
    getFile: suspend (MediaFileId) -> File
): MediaPlayer

abstract class LocalMediaPlayer(
    clientId: ClientId,
    private val getFile: suspend (MediaFileId) -> File
) : MediaPlayer() {
    protected companion object {
        const val BIT_DEPTH: Int = 16
        const val SAMPLE_RATE: Int = 48000
        const val CHANNELS: Int = 2
    }
    private val logger = KotlinLogging.logger {}
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    private val commands = Channel<PlayerCommand>(
        capacity = Channel.RENDEZVOUS,
        onUndeliveredElement = { cmd ->
            cmd.ack.completeExceptionally(CancellationException())
        }
    )
    private val loop: EventLoop = EventLoop(clientId, commands, ::createAudioOutput)
    override val state: StateFlow<MediaPlayerState> get() = loop.state
    override val playbackIntervals: Flow<PlaybackInterval> get() = loop.playbackIntervals

    override suspend fun setTrack(id: MediaFileId) {
        val file = coroutineScope.async(Dispatchers.IO) { getFile(id).resolved() }
        dispatchCommand { ack -> PlayerCommand.SetTrack(id, file, ack) }
    }

    override suspend fun play(context: PlaybackContext.ServerSent) {
        dispatchCommand { ack -> PlayerCommand.Play(ack, context) }
    }

    override suspend fun pause() {
        dispatchCommand { ack -> PlayerCommand.Pause(ack) }
    }

    override suspend fun stop() {
        dispatchCommand { ack -> PlayerCommand.Stop(ack) }
    }

    override suspend fun setPositionMs(positionMs: Long, context: PlaybackContext.ServerSent) {
        dispatchCommand { ack -> PlayerCommand.Seek(positionMs, context, ack) }
    }

    override suspend fun setState(value: MediaPlayerState, context: PlaybackContext.ServerSent?) {
        val file = coroutineScope.async(Dispatchers.IO) { value.track?.let { getFile(it).resolved() } }
        dispatchCommand { ack -> PlayerCommand.SetState(value, file, context, ack) }
    }

    override suspend fun release() {
        commands.close()
        coroutineScope.cancel()
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

    private class EventLoop(
        clientId: ClientId,
        private val commands: ReceiveChannel<PlayerCommand>,
        private val createAudioOutput: () -> AudioOutput
    ) {
        private val logger = KotlinLogging.logger {}
        private val coroutineScope = run {
            val dispatcher = Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "JavaCV-Playback-Worker").apply {
                    isDaemon = true // TODO
                }
            }.asCoroutineDispatcher()
            CoroutineScope(dispatcher + SupervisorJob())
        }

        class Audio(
            val grabber: FFmpegFrameGrabber,
            val line: AudioOutput,
            val trackId: MediaFileId
        )

        private var audio: Audio? = null
        private var isPlaying = false
        private var isFinished = false
        private val recorder = PlaybackIntervalRecorder(clientId)

        val state: StateFlow<MediaPlayerState> field = MutableStateFlow(MediaPlayerState.initial())
        val playbackIntervals: Flow<PlaybackInterval> get() = recorder.playbackIntervals

        private var FFmpegFrameGrabber?.positionMs: Long
            get() = (this?.timestamp ?: 0L) / 1000L
            set(value) {
                this?.timestamp = value * 1000L
                audio?.line?.setPositionOffset(value)
            }

        init {
            coroutineScope.launch {
                try {
                    var lastProcessedCommand: PlayerCommand? = null
                    while (isActive) {
                        val cmd = if (isPlaying) commands.tryReceive().getOrNull() else commands.receive()
                        if (cmd != null) {
                            try {
                                when (cmd) {
                                    is PlayerCommand.SetTrack -> {
                                        setTrack(cmd.id, cmd.uri)
                                        lastProcessedCommand = cmd
                                        cmd.ack.complete(Unit)
                                    }
                                    is PlayerCommand.Play -> {
                                        if (lastProcessedCommand !is PlayerCommand.Play || isFinished) {
                                            if (audio == null) TODO()
                                            play(cmd.context)
                                            emitState()
                                            lastProcessedCommand = cmd
                                        }
                                        cmd.ack.complete(Unit)
                                    }
                                    is PlayerCommand.Pause -> {
                                        if (lastProcessedCommand !is PlayerCommand.Pause) {
                                            if (audio == null) TODO()
                                            isPlaying = false
                                            audio!!.apply {
                                                line.stop()
                                                recorder.end(line.positionMs, System.currentTimeMillis())
                                            }
                                            emitState()
                                            lastProcessedCommand = cmd
                                        }
                                        cmd.ack.complete(Unit)
                                    }
                                    is PlayerCommand.Stop -> {
                                        if (lastProcessedCommand !is PlayerCommand.Stop) {
                                            if (audio == null) TODO()
                                            isPlaying = false
                                            isFinished = false
                                            audio!!.apply {
                                                line.stop()
                                                recorder.end(line.positionMs, System.currentTimeMillis())
                                                grabber.positionMs = 0L
                                            }
                                            emitState()
                                            lastProcessedCommand = cmd
                                        }
                                        cmd.ack.complete(Unit)
                                    }
                                    is PlayerCommand.Seek -> {
                                        if (
                                            lastProcessedCommand !is PlayerCommand.Seek
                                            || lastProcessedCommand.positionMs != cmd.positionMs
                                        ) {
                                            if (audio == null) TODO()
                                            seek(cmd.positionMs, cmd.context)
                                            isFinished = false
                                            emitState()
                                            lastProcessedCommand = cmd
                                        }
                                        cmd.ack.complete(Unit)
                                    }
                                    is PlayerCommand.SetState -> {
                                        if (
                                            lastProcessedCommand !is PlayerCommand.SetState
                                            || lastProcessedCommand.value != cmd.value
                                        ) {
                                            val state = cmd.value
                                            setTrack(state.track, cmd.uri)
                                            if (audio != null) {
                                                seek(state.startPositionMs, cmd.context)
                                            }
                                            if (state.isPlaying && state.track != null) {
                                                play(cmd.context!!)
                                            }
                                            emitState()
                                            lastProcessedCommand = cmd
                                        }
                                        cmd.ack.complete(Unit)
                                    }
                                }
                            } catch (e: Exception) {
                                logger.error(e) { "Error executing command" }
                                cmd.ack.completeExceptionally(e)
                            }
                        }
                        if (isPlaying) {
                            if (audio == null) TODO()
                            audio!!.apply {
                                try {
                                    val frame: Frame? = grabber.grabSamples()
                                    if (frame == null) {
                                        line.drain()
                                        recorder.end(line.positionMs, System.currentTimeMillis())
                                        isPlaying = false
                                        isFinished = true
                                        emitState()
                                        continue
                                    }
                                    val samples: ShortBuffer? = frame.samples?.firstOrNull() as? ShortBuffer
                                    if (samples != null && samples.remaining() > 0) {
                                        line.write(samples)
                                    }
                                } catch (e: Throwable) {
                                    val end = System.currentTimeMillis()
                                    logger.error(e) { "Playback error" }
                                    isPlaying = false
                                    emitState()
                                    recorder.end(line.positionMs, end)
                                }
                            }
                        }
                    }
                } finally {
                    try {
                        audio?.apply {
                            line.apply { stop(); release() }
                            recorder.end(line.positionMs, System.currentTimeMillis())
                        }
                    } catch (_: Throwable) {}
                    try { audio?.grabber?.close() } catch (_: Throwable) {}
                }
            }
        }

        private fun emitState() {
            state.value = MediaPlayerState(
                track = audio?.trackId,
                isPlaying = isPlaying,
                startPositionMs = audio?.line?.positionMs ?: 0,
                isFinished = isFinished
            )
        }

        private suspend fun setTrack(id: MediaFileId?, uri: Deferred<String?>) {
            audio?.apply {
                line.apply {
                    stop()
                    recorder.end(line.positionMs, System.currentTimeMillis())
                    release()
                }
                grabber.close()
            }
            audio = null
            isPlaying = false
            isFinished = false
            val uri = uri.await()
            if (uri != null) {
                audio = Audio(
                    grabber = FFmpegFrameGrabber(uri).apply {
                        sampleFormat = avutil.AV_SAMPLE_FMT_S16
                        sampleRate = SAMPLE_RATE
                        audioChannels = CHANNELS
                        start()
                    },
                    line = createAudioOutput(),
                    trackId = id!!
                )
            }
            emitState()
        }

        private fun play(context: PlaybackContext.ServerSent) {
            if (audio == null) TODO()
            audio!!.apply {
                if (isFinished) {
                    grabber.positionMs = 0L
                    isFinished = false
                }
                line.play()
                recorder.start(trackId, line.positionMs, System.currentTimeMillis(), context)
                isPlaying = true
            }
        }

        private fun seek(positionMs: Long, context: PlaybackContext.ServerSent?) {
            if (audio == null) TODO()
            audio!!.apply {
                recorder.end(line.positionMs, System.currentTimeMillis())
                grabber.positionMs = positionMs
                if (isPlaying) {
                    recorder.start(trackId, line.positionMs, System.currentTimeMillis(), context!!)
                }
            }
        }

        private class PlaybackIntervalRecorder(
            private val clientId: ClientId
        ) {
            private var start: Start? = null
            private val history = Channel<PlaybackInterval>(Channel.UNLIMITED)

            val playbackIntervals: Flow<PlaybackInterval> get() = flow {
                for (item in history) {
                    emit(item)
                }
            }

            fun start(
                track: MediaFileId,
                from: Long,
                startDatetime: Long,
                context: PlaybackContext.ServerSent
            ) {
                if (start != null) {
                    TODO()
                }
                start = Start(
                    clientId = clientId,
                    track = track,
                    from = from,
                    datetime = startDatetime,
                    context = context.toClientSent(clientId)
                )
            }

            fun end(to: Long, datetime: Long) {
                val active = start ?: return
                if (active.from > to) {
                    TODO()
                }
                history.trySendBlocking(
                    PlaybackInterval(
                        audioFileId = active.track,
                        context = active.context,
                        from = active.from,
                        to = to,
                        startDatetime = active.datetime,
                        endDatetime = datetime
                    )
                )
                start = null
            }

            private data class Start(
                val clientId: ClientId,
                val track: MediaFileId,
                val from: Long,
                val datetime: Long,
                val context: PlaybackContext.ClientSent
            )
        }
    }

    protected abstract fun createAudioOutput(): AudioOutput

    private sealed class PlayerCommand {
        abstract val ack: CompletableDeferred<Unit>
        data class SetTrack(val id: MediaFileId, val uri: Deferred<String>, override val ack: CompletableDeferred<Unit>) : PlayerCommand()
        data class Play(override val ack: CompletableDeferred<Unit>, val context: PlaybackContext.ServerSent) : PlayerCommand()
        data class Pause(override val ack: CompletableDeferred<Unit>) : PlayerCommand()
        data class Stop(override val ack: CompletableDeferred<Unit>) : PlayerCommand()
        data class Seek(val positionMs: Long, val context: PlaybackContext.ServerSent, override val ack: CompletableDeferred<Unit>) : PlayerCommand()
        data class SetState(val value: MediaPlayerState, val uri: Deferred<String?>, val context: PlaybackContext.ServerSent?, override val ack: CompletableDeferred<Unit>) : PlayerCommand()
    }

    protected abstract class AudioOutput {
        companion object {
            private const val SHORTS_PER_MS = (SAMPLE_RATE * CHANNELS) / 1000L
        }
        protected var totalShortsWritten: Long = 0L
        protected var positionOffsetMs: Long = 0L
        val positionMs: Long
            get() {
                val elapsedMs = totalShortsWritten / SHORTS_PER_MS
                return positionOffsetMs + elapsedMs
            }
        abstract fun play()
        abstract fun write(samples: ShortBuffer)
        abstract fun stop()
        abstract fun drain()
        abstract fun release()

        fun setPositionOffset(positionMs: Long) {
            positionOffsetMs = positionMs
            totalShortsWritten = 0L
        }
    }
}