package dev.younesgouyd.apps.music.client.multiplatform.data

import dev.younesgouyd.apps.music.client.multiplatform.createHttpClient
import dev.younesgouyd.apps.music.client.multiplatform.createMediaPlayer
import dev.younesgouyd.apps.music.common.MediaPlayer
import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.rpc.MediaPlayerRpc
import dev.younesgouyd.apps.music.common.models.rpc.Rpc
import dev.younesgouyd.apps.music.common.models.rpc.websocket.WSMessage
import dev.younesgouyd.apps.music.common.models.rpc.websocket.WsRequest
import dev.younesgouyd.apps.music.common.models.rpc.websocket.WsResponse
import dev.younesgouyd.apps.music.common.models.rpc.websocket.WsResponse.Done
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.jvm.javaio.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class Server(
    private val serverHost: String,
    private val serverFingerprint: String,
    private val password: String,
    private val tempDir: File
) {
    suspend fun connect() {
        session = httpClient.webSocketSession {
            url {
                protocol = URLProtocol.WSS
                host = serverHost
                port = 8443
                path("rpc")
            }
        }
        with(session) {
            launch {
                try {
                    while (isActive) {
                        val message = receiveDeserialized<WSMessage>()
                        handleIncomingMessage(message)
                    }
                } catch (e: Throwable) {
                    logger.error(e) { "FATAL EXCEPTION. DISCONNECTING..." }
                    throw e
                } finally {
                    jobs.values.forEach { it.cancel() }
                    jobs.clear()
                    responseChannels.values.forEach { it.close() }
                    responseChannels.clear()
                    logger.debug { "cancelled all jobs" }
                }
            }
        }
    }

    suspend inline fun <reified T> call(rpc: Rpc): T {
        val correlationId = Uuid.random()
        val channel = Channel<WsResponse>(Channel.CONFLATED)
        responseChannels[correlationId] = channel
        val rpcJson: String = json.encodeToString(rpc)
        try {
            val request: WSMessage = WsRequest.Execute(correlationId, rpc)
            logger.debug { "... ::call | request: $correlationId , rpc: $rpcJson"}
            session.sendSerialized<WSMessage>(request)
            return when (val response = channel.receive()) {
                is WsResponse.Done -> Unit as T
                is WsResponse.Data -> json.decodeFromJsonElement<T>(response.value)
                is WsResponse.Failure -> TODO()
            }
        } catch (e: CancellationException) {
            logger.debug { "--> ::call | CancellationException, requesting server to cancel $correlationId corresponding to RPC: $rpcJson" }
            requestCancel(correlationId)
            throw e
        } finally {
            responseChannels.remove(correlationId)
            channel.close()
        }
    }

    /**
     * returns an observable flow of the requested state that drops missed intermediate values if the collector is slow.
     */
    inline fun <reified T> observe(rpc: Rpc): Flow<T> {
        return flow {
            val activeStream = getOrCreateObservable<T>(rpc)
            emitAll(activeStream)
        }
    }

    suspend fun getFile(id: MediaFileId): File {
        val file = File(tempDir, id.toString())
        if (file.exists()) {
            return file
        }
        val deferred = fileCache.computeIfAbsent(id) { key ->
            session.async {
                val download = File(tempDir, "${key.value}_${Uuid.random()}.tmp")
                try {
                    logger.debug { "--> ::getFile | downloading file $id" }
                    httpClient.prepareGet("files/${key.value}").execute { response ->
                        download.outputStream().use { outStream ->
                            response.bodyAsChannel().toInputStream().use { inStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                    }
                    download.copyTo(file, overwrite = true)
                    download.delete()
                    file
                } catch (e: Throwable) {
                    logger.error(e) { "... ::getFile | failed downloading media file $id" }
                    throw e
                } finally {
                    if (download.exists()) { download.delete() }
                    if (!file.exists()) { fileCache.remove(key) }
                }
            }
        }
        return deferred.await()
    }

    suspend fun close() {
        session.close()
        httpClient.close()
        httpClient.coroutineContext.job.join()
        fileCache.clear()
    }





    // TODO: find a way to make these private
    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> PRIVATE MEMBER, DONT USE >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    val logger = KotlinLogging.logger {}
    private val httpClient = createHttpClient(
        serverHost = serverHost,
        serverFingerprint = serverFingerprint,
        password = password
    )
    private val fileCache = ConcurrentHashMap<MediaFileId, Deferred<File>>()
    @Volatile lateinit var session: DefaultClientWebSocketSession
    val responseChannels = ConcurrentHashMap<Uuid, SendChannel<WsResponse>>()
    private val jobs = ConcurrentHashMap<Uuid, Job>()
    val states = ConcurrentHashMap<Rpc, SharedFlow<Any?>>()

    val mediaPlayer: MediaPlayer = runBlocking {
        withContext(Dispatchers.Main.immediate) {
            createMediaPlayer(::getFile)
        }
    }

    private fun  DefaultClientWebSocketSession.handleIncomingMessage(message: WSMessage) {
        when (message) {
            is WsRequest -> when (message) {
                is WsRequest.Execute -> {
                    jobs[message.correlationId]?.cancel() // TODO
                    jobs[message.correlationId] = launch {
                        try {
                            process(
                                correlationId = message.correlationId,
                                rpc = message.rpc as? Rpc.ToClient ?: TODO()
                            )
                        } catch (e: CancellationException) {
                            logger.debug { "cancelled incoming RPC ${message.correlationId}, message.rpc: ${message.rpc}" }
                            throw e
                        } catch (e: Exception) {
                            logger.error(e) { "RPC execution failed for ${message.correlationId}" }
                        } finally {
                            jobs.remove(message.correlationId)
                        }
                    }
                }
                is WsRequest.Cancel -> {
                    jobs.remove(message.correlationId)?.cancel()
                }
            }
            is WsResponse -> {
                responseChannels[message.correlationId]?.trySend(message)
            }
        }
    }

    fun requestCancel(id: Uuid) {
        session.launch {
            try {
                val request: WSMessage = WsRequest.Cancel(id)
                session.sendSerialized<WSMessage>(request)
            } catch (e: Exception) {
                logger.error(e) { "::requestCancel | Cancellation Request $id failed" }
            }
        }
    }

    inline fun <reified T> getOrCreateObservable(rpc: Rpc): SharedFlow<T> {
        return states.computeIfAbsent(rpc) { key ->
            lateinit var sharedFlow: SharedFlow<T>
            sharedFlow = flow {
                val correlationId = Uuid.random()
                val channel = Channel<WsResponse>(Channel.CONFLATED)
                responseChannels[correlationId] = channel
                try {
                    val request: WSMessage = WsRequest.Execute(correlationId, rpc)
                    logger.debug { "... ::getOrCreateObservable | request: $correlationId , rpc: ${json.encodeToString(rpc)}"}
                    session.sendSerialized<WSMessage>(request)
                    for (state in channel) {
                        val parsed = when (state) {
                            is WsResponse.Done -> TODO()
                            is WsResponse.Data -> json.decodeFromJsonElement<T>(state.value)
                            is WsResponse.Failure -> TODO()
                        }
                        emit(parsed)
                    }
                } finally {
                    responseChannels.remove(correlationId)
                    channel.close()
                    requestCancel(correlationId)
                }
            }.distinctUntilChanged()
                .onCompletion { states.remove(key, sharedFlow) }
                .shareIn(
                    scope = session,
                    started = SharingStarted.WhileSubscribed(5.seconds),
                    replay = 1
                )
            sharedFlow
        } as SharedFlow<T>
    }

    private suspend fun DefaultClientWebSocketSession.process(correlationId: Uuid, rpc: Rpc.ToClient) {
        when (rpc) {
            is MediaPlayerRpc -> {
                when (rpc) {
                    is MediaPlayerRpc.ObserveState -> {
                        mediaPlayer.state.collect { sendWsResponse(correlationId, it) }
                    }
                    is MediaPlayerRpc.SetTrack -> {
                        mediaPlayer.setTrack(rpc.id)
                        sendSerialized<WSMessage>(Done(correlationId))
                    }
                    is MediaPlayerRpc.Play -> {
                        mediaPlayer.play()
                        sendSerialized<WSMessage>(Done(correlationId))
                    }
                    is MediaPlayerRpc.Pause -> {
                        mediaPlayer.pause()
                        sendSerialized<WSMessage>(Done(correlationId))
                    }
                    is MediaPlayerRpc.Stop -> {
                        mediaPlayer.stop()
                        sendSerialized<WSMessage>(Done(correlationId))
                    }
                    is MediaPlayerRpc.SetTime -> {
                        mediaPlayer.setTime(rpc.timeMillis.milliseconds)
                        sendSerialized<WSMessage>(Done(correlationId))
                    }
                    is MediaPlayerRpc.SetState -> {
                        mediaPlayer.setState(rpc.value)
                        sendSerialized<WSMessage>(Done(correlationId))
                    }
                }
            }
        }
    }

    // TODO: make this private
    suspend inline fun <reified T> DefaultClientWebSocketSession.sendWsResponse(correlationId: Uuid, data: T) {
        sendSerialized<WSMessage>(
            WsResponse.Data(
                correlationId = correlationId,
                value = json.encodeToJsonElement<T>(data)
            )
        )
    }
    // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< PRIVATE MEMBER, DONT USE <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
}