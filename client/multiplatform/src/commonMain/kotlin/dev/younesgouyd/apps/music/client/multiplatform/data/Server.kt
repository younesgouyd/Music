package dev.younesgouyd.apps.music.client.multiplatform.data

import dev.younesgouyd.apps.music.client.multiplatform.createMediaPlayer
import dev.younesgouyd.apps.music.common.MediaPlayer
import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.sha256Hash
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.CancellationException
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import kotlinx.serialization.KSerializer
import java.io.File
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class Server(
    private val serverHost: String,
    private val serverFingerprint: String,
    private val password: String,
    private val clientIdFile: File,
    private val tempDir: File,
    private val disconnection: CompletableDeferred<Unit>
) {
    private val logger = KotlinLogging.logger {}
    private val httpClient = HttpClient(OkHttp) {
        engine {
            config {
                val trustManager = object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                        verify(chain)
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

                    @Suppress("unused")
                    @Throws(CertificateException::class)
                    fun checkServerTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?,
                        host: String?
                    ): List<X509Certificate> {
                        verify(chain)
                        return chain?.filterNotNull() ?: emptyList()
                    }

                    private fun verify(chain: Array<out X509Certificate>?) {
                        if (chain.isNullOrEmpty()) throw CertificateException("Empty certificate chain")
                        val serverCert = chain[0]
                        val actualFingerprint = serverCert.sha256Hash()
                        if (!actualFingerprint.equals(serverFingerprint, ignoreCase = true)) {
                            throw CertificateException("Certificate fingerprint mismatch! Expected: $serverFingerprint, Got: $actualFingerprint")
                        }
                    }
                }
                val sslContext = SSLContext.getInstance("TLSv1.3").apply {
                    init(null, arrayOf(trustManager), SecureRandom())
                }
                sslSocketFactory(sslContext.socketFactory, trustManager)
                hostnameVerifier { _, _ -> true }
                pingInterval(10.seconds)
            }
        }
        install(Logging) { level = LogLevel.HEADERS }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(json)
//            pingInterval = 15.seconds // here it's not supported with OkHttp engine, it is instead specified above in engine config
        }
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = serverHost
                port = 8443
            }
            header(HttpHeaders.Authorization, Base64.encode(password.encodeToByteArray()))
        }
    }
    @Volatile private lateinit var session: DefaultClientWebSocketSession
    @Volatile private lateinit var clientRequestsHandler: ClientRequestsHandler
    @Volatile private var clientId: ClientId? = null

    suspend fun connect() {
        if (clientId == null) {
            withContext(Dispatchers.IO) {
                try {
                    clientId = ClientId(clientIdFile.readText().toLong())
                } catch (_: Exception) {}
            }
        }
        session = httpClient.webSocketSession {
            url {
                protocol = URLProtocol.WSS
                host = serverHost
                port = 8443
                path("rpc")
                clientId?.let { header("Client-ID", it.value.toString()) }
            }
        }
        val serverRequestsHandler = ServerRequestsHandler(session, { clientId!! }, ::getFile)
        clientRequestsHandler = ClientRequestsHandler(
            coroutineScope = session,
            tempDir = tempDir,
            download = { id, handleChannel ->
                httpClient.prepareGet("files/${id.value}") {
                    clientId?.let { header("Client-ID", it.value.toString()) }
                }.execute { response ->
                    handleChannel(response.bodyAsChannel())
                }
            }
        )
        with(session) {
            val job = launch {
                try {
                    while (isActive) {
                        val message = receiveDeserialized<WSMessage>()
                        logMessage(message, "<==")
                        when (message) {
                            is WSMessage.Registration -> {
                                launch(Dispatchers.IO) {
                                    clientIdFile.writeText(message.value.value.toString())
                                    clientId = message.value
                                }
                            }
                            is WSMessage.Request -> { serverRequestsHandler.handle(message) }
                            is WSMessage.Response -> { clientRequestsHandler.handle(message) }
                        }
                    }
                } catch (e: Throwable) {
                    logger.error(e) {}
                    throw e
                } finally {
                    serverRequestsHandler.release()
                    clientRequestsHandler.release()
                    logger.debug { "cancelled all jobs. signaling disconnection..." }
                    disconnection.complete(Unit)
                }
            }
            if (!job.isActive) {
                logger.debug { "message collection job is not active. signaling disconnection..." }
                disconnection.complete(Unit)
                return
            }
            launch {
                serverRequestsHandler.responses.consumeEach { message ->
                    logMessage(message, "==>")
                    session.sendSerialized<WSMessage>(message)
                }
            }
            launch {
                clientRequestsHandler.requests.consumeEach { message ->
                    logMessage(message, "==>")
                    session.sendSerialized<WSMessage>(message)
                }
            }
        }
    }

    suspend fun <TRpc> call(rpc: TRpc) where TRpc: Rpc.ToServer, TRpc: ResultType.Action {
        clientRequestsHandler.call(rpc)
    }

    suspend fun <TRpc, TResult> get(rpc: TRpc): TResult where TRpc: Rpc.ToServer, TRpc: ResultType.Data.Static<TResult> {
        return clientRequestsHandler.get(rpc)
    }

    fun <TRpc, TResult> observe(rpc: TRpc): Flow<TResult> where TRpc: Rpc.ToServer, TRpc: ResultType.Data.Dynamic<TResult> {
        return clientRequestsHandler.observe(rpc)
    }

    suspend fun getFile(id: MediaFileId): File {
        return clientRequestsHandler.getFile(id)
    }

    suspend fun close() {
        try { session.close() } catch (_: Throwable) {}
        httpClient.close()
        httpClient.coroutineContext.job.join()
    }

    private fun logMessage(message: WSMessage, direction: String) {
        val logMessage: String = when (message) {
            is WSMessage.Registration -> {
                "Registration ClientId: ${message.value}"
            }
            is WSMessage.Request -> {
                when (message) {
                    is WSMessage.Request.Execute -> "Request.Execute ${message.correlationId}: ${json.encodeToString(message.rpc)}"
                    is WSMessage.Request.Cancel -> "Request.Cancel ${message.correlationId}"
                }
            }
            is WSMessage.Response -> {
                when (message) {
                    is WSMessage.Response.Data -> "Response.Data ${message.correlationId}: ${json.encodeToString(message.value)}"
                    is WSMessage.Response.Done -> "Response.Data ${message.correlationId}"
                }
            }
        }
        logger.debug { "$direction $logMessage" }
    }

    private class ClientRequestsHandler(
        private val coroutineScope: CoroutineScope,
        tempDir: File,
        download: suspend (MediaFileId, handleChannel: suspend (ByteReadChannel) -> Unit) -> Unit
    ) {
        private val responseChannels = ConcurrentHashMap<Uuid, SendChannel<WSMessage.Response>>()
        private val states = ConcurrentHashMap<Rpc.ToServer, SharedFlow<Any?>>()
        private val fileManager: FileManager = FileManager(tempDir, coroutineScope, download)

        val requests: ReceiveChannel<WSMessage.Request>
            field = Channel<WSMessage.Request>(Channel.UNLIMITED)

        fun handle(response: WSMessage.Response) {
            responseChannels[response.correlationId]?.trySend(response)
        }

        suspend fun <TRpc> call(rpc: TRpc) where TRpc: Rpc.ToServer, TRpc: ResultType.Action {
            val correlationId = Uuid.random()
            val channel = Channel<WSMessage.Response>(Channel.CONFLATED)
            responseChannels[correlationId] = channel
            try {
                val request: WSMessage.Request = WSMessage.Request.Execute(correlationId, rpc)
                requests.send(request)
                when (channel.receive()) {
                    is WSMessage.Response.Done -> return
                    is WSMessage.Response.Data -> { TODO() }
                }
            } catch (e: CancellationException) {
                requests.send(WSMessage.Request.Cancel(correlationId))
                throw e
            } finally {
                responseChannels.remove(correlationId)
                channel.close()
            }
        }

        suspend fun <TRpc, TResult> get(rpc: TRpc): TResult where TRpc: Rpc.ToServer, TRpc: ResultType.Data.Static<TResult> {
            val correlationId = Uuid.random()
            val channel = Channel<WSMessage.Response>(Channel.CONFLATED)
            responseChannels[correlationId] = channel
            try {
                val request: WSMessage.Request = WSMessage.Request.Execute(correlationId, rpc)
                requests.send(request)
                return when (val response = channel.receive()) {
                    is WSMessage.Response.Done -> TODO()
                    is WSMessage.Response.Data -> { json.decodeFromJsonElement(rpc.serializer, response.value) }
                }
            } catch (e: CancellationException) {
                requests.send(WSMessage.Request.Cancel(correlationId))
                throw e
            } finally {
                responseChannels.remove(correlationId)
                channel.close()
            }
        }

        fun <TRpc, TResult> observe(rpc: TRpc): Flow<TResult> where TRpc: Rpc.ToServer, TRpc: ResultType.Data.Dynamic<TResult> {
            return flow {
                val activeStream = states.computeIfAbsent(rpc) { key ->
                    lateinit var sharedFlow: SharedFlow<TResult>
                    sharedFlow = flow {
                        val correlationId = Uuid.random()
                        val channel = Channel<WSMessage.Response>(Channel.CONFLATED)
                        responseChannels[correlationId] = channel
                        try {
                            val request: WSMessage.Request = WSMessage.Request.Execute(correlationId, rpc)
                            requests.send(request)
                            for (state in channel) {
                                val parsed = when (state) {
                                    is WSMessage.Response.Done -> TODO()
                                    is WSMessage.Response.Data -> json.decodeFromJsonElement(rpc.serializer, state.value)
                                }
                                emit(parsed)
                            }
                        } finally {
                            responseChannels.remove(correlationId)
                            channel.close()
                            requests.send(WSMessage.Request.Cancel(correlationId))
                        }
                    }.distinctUntilChanged()
                        .onCompletion { states.remove(key, sharedFlow) }
                        .shareIn(
                            scope = coroutineScope,
                            started = SharingStarted.WhileSubscribed(5.seconds),
                            replay = 1
                        )
                    sharedFlow
                } as SharedFlow<TResult>
                emitAll(activeStream)
            }
        }

        fun release() {
            responseChannels.values.forEach { it.close() }
            responseChannels.clear()
            fileManager.release()
        }

        suspend fun getFile(id: MediaFileId): File {
            return fileManager.get(id)
        }

        private class FileManager(
            private val tempDir: File,
            private val coroutineScope: CoroutineScope,
            private val download: suspend (MediaFileId, handleChannel: suspend (ByteReadChannel) -> Unit) -> Unit
        ) {
            private val logger = KotlinLogging.logger {}
            private val fileCache = ConcurrentHashMap<MediaFileId, Deferred<File>>()

            suspend fun get(id: MediaFileId): File {
                val file = File(tempDir, id.toString())
                if (file.exists()) {
                    return file
                }
                val deferred = fileCache.computeIfAbsent(id) { key ->
                    coroutineScope.async(Dispatchers.IO) {
                        val tempFile = File(tempDir, "${key.value}_${Uuid.random()}.tmp")
                        try {
                            logger.debug { "--> ::get | downloading file $id" }
                            download(id) { it.copyAndClose(tempFile.writeChannel()) }
                            tempFile.copyTo(file, overwrite = true)
                            tempFile.delete()
                            file
                        } catch (e: Throwable) {
                            logger.error(e) { "... ::get | failed downloading media file $id" }
                            throw e
                        } finally {
                            if (tempFile.exists()) { tempFile.delete() }
                            if (!file.exists()) { fileCache.remove(key) }
                        }
                    }
                }
                return deferred.await()
            }

            fun release() {
                fileCache.clear()
            }
        }
    }

    private class ServerRequestsHandler(
        private val coroutineScope: CoroutineScope,
        clientId: () -> ClientId,
        getFile: suspend (MediaFileId) -> File
    ) {
        private val jobs = ConcurrentHashMap<Uuid, Job>()
        private val mediaPlayer: MediaPlayer by lazy { createMediaPlayer(clientId(), getFile) }

        val responses: ReceiveChannel<WSMessage.Response>
            field = Channel<WSMessage.Response>(Channel.UNLIMITED)

        fun handle(request: WSMessage.Request) {
            when (request) {
                is WSMessage.Request.Execute -> {
                    jobs[request.correlationId]?.cancel() // TODO
                    jobs[request.correlationId] = coroutineScope.launch {
                        try {
                            process(
                                correlationId = request.correlationId,
                                rpc = request.rpc as? Rpc.ToClient ?: TODO()
                            )
                        } finally {
                            jobs.remove(request.correlationId)
                        }
                    }
                }
                is WSMessage.Request.Cancel -> {
                    jobs.remove(request.correlationId)?.cancel()
                }
            }
        }

        fun release() { // TODO
            jobs.values.forEach { it.cancel() }
            jobs.clear()
        }

        private suspend fun process(correlationId: Uuid, rpc: Rpc.ToClient) {
            when (rpc) {
                is Rpc.ToClient.MediaPlayer -> {
                    when (rpc) {
                        is Rpc.ToClient.MediaPlayer.ObserveState -> {
                            mediaPlayer.state.collect {
                                sendData(correlationId, it, rpc.serializer)
                            }
                        }
                        is Rpc.ToClient.MediaPlayer.SetTrack -> {
                            mediaPlayer.setTrack(rpc.id)
                            sendDone(correlationId)
                        }
                        is Rpc.ToClient.MediaPlayer.Play -> {
                            mediaPlayer.play(rpc.context)
                            sendDone(correlationId)
                        }
                        is Rpc.ToClient.MediaPlayer.Pause -> {
                            mediaPlayer.pause()
                            sendDone(correlationId)
                        }
                        is Rpc.ToClient.MediaPlayer.Stop -> {
                            mediaPlayer.stop()
                            sendDone(correlationId)
                        }
                        is Rpc.ToClient.MediaPlayer.SetTime -> {
                            mediaPlayer.setPositionMs(rpc.positionMs, rpc.context)
                            sendDone(correlationId)
                        }
                        is Rpc.ToClient.MediaPlayer.SetState -> {
                            mediaPlayer.setState(rpc.value, rpc.context)
                            sendDone(correlationId)
                        }
                        is Rpc.ToClient.MediaPlayer.CollectPlaybackIntervals -> {
                            mediaPlayer.playbackIntervals.collect {
                                sendData(correlationId, it, rpc.serializer)
                            }
                        }
                    }
                }
            }
        }

        private suspend fun <T> sendData(correlationId: Uuid, data: T, serializer: KSerializer<T>) {
            responses.send(
                WSMessage.Response.Data(
                    correlationId = correlationId,
                    value = json.encodeToJsonElement(serializer, data)
                )
            )
        }

        private suspend fun sendDone(correlationId: Uuid) {
            responses.send(
                WSMessage.Response.Done(correlationId)
            )
        }
    }
}