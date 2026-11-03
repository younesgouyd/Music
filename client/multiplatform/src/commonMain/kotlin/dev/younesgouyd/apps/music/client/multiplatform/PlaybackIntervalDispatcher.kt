package dev.younesgouyd.apps.music.client.multiplatform

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.datamodels.PlaybackInterval
import dev.younesgouyd.apps.music.common.models.datamodels.PlaybackIntervalStart
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.file.Files
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.cancellation.CancellationException
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalAtomicApi::class)
class PlaybackIntervalDispatcher(
    private val server: Server,
    private val source: ReceiveChannel<PlaybackInterval>,
    private val starts: ReceiveChannel<PlaybackIntervalStart>,
    private val playbackIntervalsDir: File
) {
    private val logger = KotlinLogging.logger {  }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex() // used for atomic file io
    private val failedDir = File(playbackIntervalsDir, "failed")
    private val started = AtomicBoolean(false)
    @Volatile private var writeJob: Job? = null
    @Volatile private var sendJob: Job? = null
    @Volatile private var startsJob: Job? = null
    private val shouldSend = AtomicBoolean(true)

    fun start() {
        scope.launch {
            if (started.compareAndSet(expectedValue = false, newValue = true)) {
                playbackIntervalsDir.mkdir()
                failedDir.mkdir()
                playbackIntervalsDir.listFiles()
                    .orEmpty()
                    .filter { it.isFile && it.extension == "tmp" }
                    .forEach { it.delete() }
                writeJob = scope.launch {
                    // used consumeEach to close the channel for send upon failure of processing an item.
                    // this coroutine must complete normally upon close and draining of all items,
                    // so the owner of "source" is the one that controls the lifecycle of this coroutine.
                    // intended behavior: crash the app if any exception occurs such as an io exception.
                    source.consumeEach { item ->
                        mutex.withLock {
                            try {
                                val file = File(playbackIntervalsDir, "${item.uuid}.tmp")
                                val jsonStr = json.encodeToString<PlaybackInterval>(item)
                                file.writeText(jsonStr)
                                Files.move(
                                    file.toPath(),
                                    File(playbackIntervalsDir, "${item.uuid}.json").toPath()
                                )
                                shouldSend.store(true)
                            } catch (e: Exception) {
                                logger.error(e) { "could not write PlaybackInterval to file. shutting down..." }
                                exitProcess(1) // TODO: implement a proper shutdown
                            }
                        }
                        shouldSend.store(true)
                    }
                    logger.info { "written all remaining items." }
                }
                sendJob = scope.launch {
                    logger.debug { "--> sendJob" }
                    while (isActive) {
                        try {
                            if (shouldSend.compareAndSet(expectedValue = true, newValue = false)) {
                                send()
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            // the server should always return success whether the item is valid or not. so this
                            // exception should only be interpreted as a network exception.
                            // also the server will check for duplicate items by the PlaybackInterval.uuid, ignore them, and return success.
                            logger.warn(e) { "send failed" }
                            break // stop sending until application restart
                        }
                        delay(10.seconds)
                    }
                }
                startsJob = scope.launch {
                    starts.consumeEach {
                        try {
                            server.call(
                                Rpc.ToServer.SetPlaybackIntervalStart(it)
                            )
                        } catch (e: Exception) {
                            logger.error(e) {}
                        }
                    }
                }
            }
        }
    }

    fun stopSending() {
        shouldSend.store(false)
        sendJob?.cancel()
    }

    suspend fun waitRemaining() {
        writeJob?.join()
    }

    private suspend fun send() {
        logger.debug { "--> ::send" }
        var count = 0
        try {
            val files = mutex.withLock {
                // order doesn't matter as long as all items are eventually sent
                playbackIntervalsDir.listFiles()
                    .orEmpty()
                    .filter { it.isFile && it.extension == "json" }
            }
            logger.debug { "... ::send | files.size: ${files.size}" }
            for (file in files) {
                val item: PlaybackInterval? = mutex.withLock {
                    try {
                        json.decodeFromString<PlaybackInterval>(file.readText())
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.error(e) { "failed deserializing item" }
                        val targetName = "${file.nameWithoutExtension}_${System.currentTimeMillis()}.${file.extension}"
                        Files.move(
                            file.toPath(),
                            File(failedDir, targetName).toPath()
                        )
                        null
                    }
                }
                if (item != null) {
                    server.call(
                        Rpc.ToServer.PlaybackInterval.Add(item)
                    )
                    file.delete()
                    count++
                }
            }
        } finally {
            logger.debug { "sent $count PlaybackIntervals" }
        }
    }
}