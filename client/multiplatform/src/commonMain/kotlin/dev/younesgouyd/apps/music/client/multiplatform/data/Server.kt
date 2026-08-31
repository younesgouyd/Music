package dev.younesgouyd.apps.music.client.multiplatform.data

import dev.younesgouyd.apps.music.client.multiplatform.createHttpClient
import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.rpc.Rpc
import dev.younesgouyd.apps.music.common.models.rpc.websocket.WsRequest
import dev.younesgouyd.apps.music.common.models.rpc.websocket.WsResponse
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
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class Server(
    private val serverHost: String,
    private val serverFingerprint: String,
    private val password: String,
    private val tempDir: File
) {
    suspend fun connect() {
        val currentSession = httpClient.webSocketSession {
            url {
                protocol = URLProtocol.WSS
                host = serverHost
                port = 8443
                path("rpc")
            }
        }
        session = currentSession
        coroutineScope.launch {
            try {
                while (currentSession.isActive) {
                    val response = currentSession.receiveDeserialized<WsResponse>()
                    channels[response.correlationId]?.send(response)
                }
            } catch (e: Exception) {
                channels.values.forEach { it.close(e) }
                channels.clear()
            } finally {
                session = null
            }
        }
    }

//    suspend fun call(rpc: Rpc) {
//        val currentSession = session ?: error("WebSocket not connected")
//        val request: WsRequest = WsRequest.Execute(Uuid.random(), rpc)
//        logger.debug { "... ::call | request : ${json.encodeToString(request)}"}
//        currentSession.sendSerialized<WsRequest>(request)
//    }

    suspend inline fun <reified T> call(rpc: Rpc): T {
        val currentSession = session ?: error("WebSocket not connected")
        val correlationId = Uuid.random()
        val channel = Channel<WsResponse>(Channel.CONFLATED)
        channels[correlationId] = channel
        try {
            val request = WsRequest.Execute(correlationId, rpc)
            logger.debug { "... ::call | request : ${json.encodeToString(request)}"}
            currentSession.sendSerialized<WsRequest>(request)
            return when (val response = channel.receive()) {
                is WsResponse.Done -> Unit as T
                is WsResponse.Data -> json.decodeFromJsonElement<T>(response.value)
                is WsResponse.Failure -> TODO()
            }
        } catch (e: CancellationException) {
            logger.debug { "--> ::call | CancellationException, requesting server to cancel $correlationId" }
            requestCancel(correlationId)
            throw e
        } finally {
            channels.remove(correlationId)
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
        logger.debug { "--> ::getFile(id: $id)" }
        val file = File(tempDir, id.toString())
        if (file.exists()) {
            logger.debug { "--> ::getFile | returning cached media file $id" }
            return file
        }
        val deferred = fileCache.computeIfAbsent(id) { key ->
            coroutineScope.async {
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
        session?.close()
        session = null
        httpClient.close()
        httpClient.coroutineContext.job.join()
        fileCache.clear()
        coroutineScope.cancel()
    }





    // TODO: find a way to make these private
    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> PRIVATE MEMBER, DONT USE >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val logger = KotlinLogging.logger {}
    private val httpClient = createHttpClient(
        serverHost = serverHost,
        serverFingerprint = serverFingerprint,
        password = password
    )
    private val fileCache = ConcurrentHashMap<MediaFileId, Deferred<File>>()
    @Volatile var session: DefaultClientWebSocketSession? = null
    val channels = ConcurrentHashMap<Uuid, SendChannel<WsResponse>>()
    val streams = ConcurrentHashMap<Rpc, SharedFlow<Any?>>()

    fun requestCancel(id: Uuid) {
        val currentSession = session ?: return
        coroutineScope.launch {
            try {
                val request = WsRequest.Cancel(id)
                logger.debug { "... ::requestCancel | request : ${json.encodeToString(request)}"}
                currentSession.sendSerialized<WsRequest>(request)
            } catch (e: Exception) {
                logger.error(e) { "::requestCancel | Cancellation Request $id failed" }
            }
        }
    }

    inline fun <reified T> getOrCreateObservable(rpc: Rpc): SharedFlow<T> {
        return streams.computeIfAbsent(rpc) { key ->
            lateinit var sharedFlow: SharedFlow<T>
            sharedFlow = flow {
                val currentSession = session ?: error("WebSocket not connected")
                val correlationId = Uuid.random()
                val channel = Channel<WsResponse>(Channel.CONFLATED)
                channels[correlationId] = channel
                try {
                    val request = WsRequest.Execute(correlationId, rpc)
                    logger.debug { "... ::getOrCreateObservable | request: ${json.encodeToString(request)}"}
                    currentSession.sendSerialized<WsRequest>(request)
                    for (state in channel) {
                        val parsed = when (state) {
                            is WsResponse.Done -> TODO()
                            is WsResponse.Data -> json.decodeFromJsonElement<T>(state.value)
                            is WsResponse.Failure -> TODO()
                        }
                        emit(parsed)
                    }
                } finally {
                    channels.remove(correlationId)
                    channel.close()
                    requestCancel(correlationId)
                }
            }.distinctUntilChanged()
                .onCompletion { streams.remove(key, sharedFlow) }
                .shareIn(
                    scope = coroutineScope,
                    started = SharingStarted.WhileSubscribed(5.seconds),
                    replay = 1
                )
            sharedFlow
        } as SharedFlow<T>
    }
    // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< PRIVATE MEMBER, DONT USE <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
}