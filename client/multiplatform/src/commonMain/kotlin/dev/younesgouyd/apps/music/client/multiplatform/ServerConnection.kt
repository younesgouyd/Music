package dev.younesgouyd.apps.music.client.multiplatform

import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.WSMessage
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
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import java.io.File
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.seconds

class ServerConnection(
    private val serverHost: String,
    serverFingerprint: String,
    password: String,
    private val clientIdFile: File
) {
    private val logger = KotlinLogging.logger {}
    private val httpClient = HttpClient(OkHttp) {
        engine {
            config {
                val trustManager = TrustManager(serverFingerprint)
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
    private lateinit var session: DefaultClientWebSocketSession

    private val outgoingMessages: Channel<WSMessage> = Channel(Channel.UNLIMITED)

    val clientId: Deferred<ClientId> field = CompletableDeferred()
    val disconnection: Deferred<Unit> field = CompletableDeferred()
    val requests: ReceiveChannel<WSMessage.Request> field = Channel(Channel.UNLIMITED)
    val responses: ReceiveChannel<WSMessage.Response> field = Channel(Channel.UNLIMITED)

    // suspends until clientId is set
    suspend fun connect() {
        val savedClientId = runCatching {
            withContext(Dispatchers.IO) { ClientId(clientIdFile.readText().toLong()) }
        }.getOrNull()
        if (savedClientId != null) {
            clientId.complete(savedClientId)
        }
        session = httpClient.webSocketSession {
            url {
                protocol = URLProtocol.WSS
                host = serverHost
                port = 8443
                path("rpc")
                if (savedClientId != null) {
                    header("Client-ID", savedClientId.value.toString())
                }
            }
        }
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
                                    clientId.complete(message.value)
                                }
                            }
                            is WSMessage.Request -> { requests.send(message) }
                            is WSMessage.Response -> { responses.send(message) }
                        }
                    }
                } catch (e: Throwable) {
                    logger.error(e) {}
                    throw e
                } finally {
                    logger.debug { "signaling disconnection..." }
                    disconnection.complete(Unit)
                }
            }
            if (!job.isActive) {
                logger.debug { "message collection job is not active. signaling disconnection..." }
                disconnection.complete(Unit)
                return
            }
            clientId.await() // suspend connect() until the clientId is set
            launch {
                outgoingMessages.consumeEach { message ->
                    logMessage(message, "==>")
                    session.sendSerialized<WSMessage>(message)
                }
            }
        }
    }

    fun send(message: WSMessage) {
        outgoingMessages.trySend(message)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun downloadTo(id: MediaFileId, file: File) {
        httpClient.prepareGet("files/${id.value}") {
            header("Client-ID", clientId.getCompleted().toString())
        }.execute { response ->
            response.bodyAsChannel()
                .copyAndClose(
                    file.writeChannel()
                )
        }
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

    @Suppress("CustomX509TrustManager")
    private class TrustManager(
        private val serverFingerprint: String
    ): X509TrustManager {
        @Suppress("TrustAllX509TrustManager")
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
}