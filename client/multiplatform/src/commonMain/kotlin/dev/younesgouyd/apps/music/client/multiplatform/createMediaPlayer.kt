package dev.younesgouyd.apps.music.client.multiplatform

import dev.younesgouyd.apps.music.common.MediaPlayer
import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.datamodels.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.internal.closeQuietly
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import java.io.File
import java.nio.ShortBuffer
import java.util.concurrent.Executors
import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.Uuid

expect fun createMediaPlayer(
    clientId: ClientId,
    playbackIntervals: SendChannel<PlaybackInterval>,
    playbackIntervalsStarts: SendChannel<PlaybackIntervalStart>,
    getFile: suspend (MediaFileId) -> File
): MediaPlayer

@OptIn(ExperimentalAtomicApi::class)
abstract class LocalMediaPlayer(
    clientId: ClientId,
    playbackIntervals: SendChannel<PlaybackInterval>,
    playbackIntervalsStarts: SendChannel<PlaybackIntervalStart>,
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
    private val released = AtomicBoolean(false)
    private val commands = Channel<PlayerCommand>(
        capacity = Channel.RENDEZVOUS,
        onUndeliveredElement = { cmd ->
            cmd.ack.completeExceptionally(CancellationException())
        }
    )
    private val loop: EventLoop = EventLoop(
        clientId = clientId,
        commands = commands,
        playbackIntervals = playbackIntervals,
        playbackIntervalsStarts = playbackIntervalsStarts,
        createAudioOutput = ::createAudioOutput
    )
    override val state: StateFlow<MediaPlayerState> get() = loop.state

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
        if (released.compareAndSet(expectedValue = false, newValue = true)) {
            mutex.withLock {
                commands.cancel()
                loop.release()
                coroutineScope.cancel()
            }
        }
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

    @OptIn(ExperimentalAtomicApi::class)
    private class EventLoop(
        clientId: ClientId,
        private val commands: ReceiveChannel<PlayerCommand>,
        playbackIntervals: SendChannel<PlaybackInterval>,
        playbackIntervalsStarts: SendChannel<PlaybackIntervalStart>,
        private val createAudioOutput: () -> AudioOutput
    ) {
        private val logger = KotlinLogging.logger {}
        private val dispatcher = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "Media-Player-Worker").apply {
                isDaemon = true // TODO
            }
        }.asCoroutineDispatcher()
        private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

        private var audio: Audio? = null
        private var isPlaying = false
        private var isFinished = false
        private val recorder = PlaybackIntervalRecorder(
            clientId = clientId,
            channel = playbackIntervals,
            starts = playbackIntervalsStarts
        )
        private val released = AtomicBoolean(false)
        @Volatile private var stopFlag = false
        private val stopped = CompletableDeferred<Unit>()

        val state: StateFlow<MediaPlayerState> field = MutableStateFlow(MediaPlayerState.initial())

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
                    while (isActive && !stopFlag) {
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
                                        isPlaying = false
                                        audio?.apply {
                                            line.stop()
                                            recorder.end(line.positionMs, System.currentTimeMillis())
                                        }
                                        emitState()
                                        lastProcessedCommand = cmd
                                        cmd.ack.complete(Unit)
                                    }
                                    is PlayerCommand.Stop -> {
                                        isPlaying = false
                                        isFinished = false
                                        audio?.apply {
                                            line.stop()
                                            recorder.end(line.positionMs, System.currentTimeMillis())
                                            grabber.positionMs = 0L
                                        }
                                        emitState()
                                        lastProcessedCommand = cmd
                                        cmd.ack.complete(Unit)
                                    }
                                    is PlayerCommand.Seek -> {
                                        seek(cmd.positionMs, cmd.context)
                                        emitState()
                                        lastProcessedCommand = cmd
                                        cmd.ack.complete(Unit)
                                    }
                                    is PlayerCommand.SetState -> {
                                        val state = cmd.value
                                        setTrack(state.track, cmd.uri)
                                        seek(state.startPositionMs, cmd.context)
                                        if (
                                            state.isPlaying
                                            && cmd.value.track != null
                                            && state.track != null
                                            && state.track == cmd.value.track
                                            && cmd.context != null
                                        ) {
                                            play(cmd.context)
                                        }
                                        emitState()
                                        lastProcessedCommand = cmd
                                        cmd.ack.complete(Unit)
                                    }
                                }
                            } catch (e: Exception) {
                                logger.error(e) { "Error executing command" }
                                cmd.ack.completeExceptionally(e)
                                throw e
                            }
                        }
                        if (isPlaying) {
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
                    audio?.apply {
                        isPlaying = false
                        isFinished = false
                        runCatching { line.stop(); line.release() }
                        runCatching { recorder.end(line.positionMs, System.currentTimeMillis()) }
                        runCatching { grabber.close() }
                    }
                    audio = null
                    stopped.complete(Unit)
                }
            }
        }

        suspend fun release() {
            if (released.compareAndSet(expectedValue = false, newValue = true)) {
                stopFlag = true
                stopped.await()
                coroutineScope.coroutineContext.job.cancelAndJoin()
                dispatcher.closeQuietly()
                recorder.release()
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
            audio?.apply {
                recorder.end(line.positionMs, System.currentTimeMillis())
                grabber.positionMs = positionMs
                if (isPlaying) {
                    recorder.start(trackId, line.positionMs, System.currentTimeMillis(), context!!)
                }
            }
            isFinished = false
        }

        class Audio(
            val grabber: FFmpegFrameGrabber,
            val line: AudioOutput,
            val trackId: MediaFileId
        )

        private class PlaybackIntervalRecorder(
            private val clientId: ClientId,
            private val channel: SendChannel<PlaybackInterval>,
            private val starts: SendChannel<PlaybackIntervalStart>
        ) {
            private var start: PlaybackIntervalStart? = null

            fun start(
                track: MediaFileId,
                from: Long,
                datetime: Long,
                context: PlaybackContext.ServerSent
            ) {
                if (start != null) {
                    TODO()
                }
                val s = PlaybackIntervalStart(
                    uuid = Uuid.random(),
                    clientId = clientId,
                    audioFileId = track,
                    from = from,
                    datetime = datetime,
                    context = context.toClientSent(clientId)
                )
                start = s
                starts.trySendBlocking(s)
            }

            fun end(to: Long, datetime: Long) {
                val active = start ?: return
                if (active.from > to) {
                    TODO()
                }
                channel.trySendBlocking(
                    PlaybackInterval(
                        uuid = active.uuid,
                        audioFileId = active.audioFileId,
                        context = active.context,
                        from = active.from,
                        to = to,
                        startDatetime = active.datetime,
                        endDatetime = datetime
                    )
                ).getOrThrow()
                start = null
            }

            fun release() {
                if (start != null) TODO()
                channel.close()
            }
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