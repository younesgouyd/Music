package dev.younesgouyd.apps.music.client.common.data

import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.rpc.Rpc
import dev.younesgouyd.apps.music.common.models.rpc.websocket.WsRequest
import dev.younesgouyd.apps.music.common.models.rpc.websocket.WsResponse
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.jvm.javaio.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class Backend(
    private val serverHost: String,
    private val serverPort: Int,
    private val tempDir: File
) {

    // TODO
    // DON'T USE
    val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val httpClient = HttpClient(CIO) {
        install(Logging) { level = LogLevel.ALL }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(json)
        }
        defaultRequest {
            url {
                this.protocol = URLProtocol.HTTP
                this.host = serverHost
                this.port = serverPort
            }
        }
    }
    private val fileCache = ConcurrentHashMap<MediaFileId, Deferred<File>>()

    // TODO
    @Volatile
    var session: DefaultClientWebSocketSession? = null // DON'T USE
    val channels = ConcurrentHashMap<Uuid, SendChannel<WsResponse<JsonElement>>>() // DON'T USE
    val streams = ConcurrentHashMap<Rpc, SharedFlow<Any?>>() // DON'T USE

    suspend fun connect() {
        val currentSession = httpClient.webSocketSession {
            url {
                this.protocol = URLProtocol.WS
                this.host = serverHost
                this.port = serverPort
                this.path("rpc")
            }
        }
        session = currentSession
        coroutineScope.launch {
            try {
                while (currentSession.isActive) {
                    val response = currentSession.receiveDeserialized<WsResponse<JsonElement>>()
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

    suspend fun call(rpc: Rpc) {
        val currentSession = session ?: error("WebSocket not connected")
        currentSession.sendSerialized<WsRequest>(WsRequest.Execute(Uuid.random(), rpc))
    }

    suspend inline fun <reified T> callForResult(rpc: Rpc): T {
        val currentSession = session ?: error("WebSocket not connected")
        val correlationId = Uuid.random()
        val channel = Channel<WsResponse<JsonElement>>(Channel.UNLIMITED)
        channels[correlationId] = channel
        return try {
            currentSession.sendSerialized<WsRequest>(WsRequest.Execute(correlationId, rpc))
            val response = channel.receive()
            json.decodeFromJsonElement<T>(response.data)
        } catch (e: CancellationException) {
            requestCancel(correlationId)
            throw e
        } finally {
            channels.remove(correlationId)
            channel.close()
        }
    }

    inline fun <reified T> stream(rpc: Rpc): SharedFlow<T> {
        return streams.computeIfAbsent(rpc) { key ->
            lateinit var sharedFlow: SharedFlow<Any?>
            val coldFlow = callbackFlow<WsResponse<JsonElement>> {
                val correlationId = Uuid.random()
                channels[correlationId] = channel
                val currentSession = session ?: error("WebSocket not connected")
                currentSession.sendSerialized<WsRequest>(WsRequest.Execute(correlationId, rpc))
                awaitClose {
                    requestCancel(correlationId)
                    channels.remove(correlationId)
                }
            }
            sharedFlow = coldFlow
                .map { json.decodeFromJsonElement<T>(it.data) }
                .onStart {
                    streams.putIfAbsent(key, sharedFlow)
                }
                .onCompletion {
                    streams.remove(key, sharedFlow)
                }
                .shareIn(
                    scope = coroutineScope,
                    started = SharingStarted.WhileSubscribed(stopTimeout = 5.seconds),
                    replay = 1
                ) as SharedFlow<Any?>
            sharedFlow
        } as SharedFlow<T>
    }

    suspend fun getFile(id: MediaFileId): File {
        val file = File(tempDir, id.toString())
        if (file.exists()) {
            return file
        }
        val deferred = fileCache.computeIfAbsent(id) { key ->
            coroutineScope.async {
                try {
                    val download = File(tempDir, "${key.value}_${Uuid.random()}.tmp")
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
                } catch (e: Exception) {
                    fileCache.remove(key)
                    throw e
                }
            }
        }
        return deferred.await()
    }

    // TODO
    // DON'T USE
    fun requestCancel(id: Uuid) {
        val currentSession = session ?: return
        coroutineScope.launch {
            try {
                currentSession.sendSerialized<WsRequest>(WsRequest.Cancel(id))
            } catch (_: Exception) { }
        }
    }

    suspend fun close() {
        session?.close()
        session = null
        httpClient.close()
        httpClient.coroutineContext.job.join()
        fileCache.clear()
        coroutineScope.cancel()
    }
}