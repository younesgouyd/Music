package dev.younesgouyd.apps.music.ytdlp

import dev.younesgouyd.apps.music.common.*
import dev.younesgouyd.apps.music.common.models.Inspection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.network.tls.certificates.*
import io.ktor.network.tls.extensions.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.application.Application
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.conscrypt.Conscrypt
import org.slf4j.event.Level
import java.io.File
import java.security.Security
import java.security.cert.X509Certificate
import javax.security.auth.x500.X500Principal

class Application {
    val state get() = _state.asStateFlow()
    val certificateFingerprint get() = _certificateFingerprint.asStateFlow()
    val password get() = _password.asStateFlow()

    private val logger = KotlinLogging.logger {}
    private val _state: MutableStateFlow<ServerState> = MutableStateFlow(ServerState.Stopped)
    private val _certificateFingerprint: MutableStateFlow<String> = MutableStateFlow("")
    private val _password: MutableStateFlow<String> = MutableStateFlow("")

    private lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

    fun start() {
        if (_state.value == ServerState.Starting || _state.value == ServerState.Stopping) return
        logger.info { "--> Application::start" }
        _state.value = ServerState.Starting
        _password.value = generatePassword()
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
        server = embeddedServer(
            factory = Netty,
            configure = {
                val alias = "servercertificate"
                val keyStorePassword = generatePassword()
                val keyStore = buildKeyStore {
                    certificate(alias) {
                        hash = HashAlgorithm.SHA256
                        sign = SignatureAlgorithm.RSA
                        this.password = keyStorePassword
                        subject = X500Principal("CN=localhost, O=Music, C=MA")
                        daysValid = 3
                        keySizeInBits = 2048
                        keyType = KeyType.Server
                    }
                }
                _certificateFingerprint.value = run {
                    val cert = keyStore.getCertificate(alias) as? X509Certificate ?: error("Certificate with alias '$alias' not found in KeyStore.")
                    cert.sha256Hash()
                }
                sslConnector(
                    keyStore = keyStore,
                    keyAlias = alias,
                    keyStorePassword = { keyStorePassword.toCharArray() },
                    privateKeyPassword = { keyStorePassword.toCharArray() }
                ) {
                    port = 8443
                    enabledProtocols = listOf("TLSv1.3")
                }
            }
        ) {
            install(CallLogging) {
                level = Level.TRACE
                this.format { call ->
                    val status = call.response.status()
                    val httpMethod = call.request.httpMethod.value
                    val userAgent = call.request.headers["User-Agent"]
                    "Status: $status, HTTP method: $httpMethod, User agent: $userAgent"
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(SSE)
            configureRouting()
        }
        server.start()
        _state.value = ServerState.Running
        logger.info { "<-- Application::start" }
    }

    suspend fun stop() {
        if (_state.value == ServerState.Stopping || _state.value == ServerState.Stopped) return
        logger.info { "--> Application::stop" }
        withContext(Dispatchers.IO) {
            _state.value = ServerState.Stopping
            try { server.stopSuspend(1000, 1000) } catch (_: Exception) { }
            _state.value = ServerState.Stopped
        }
        logger.info { "<-- Application::stop" }
    }

    private fun Application.configureRouting() {
        routing {
            route("/Magic") {
                get("/inspect") {
                    if (!call.verifyPassword(_password.value)) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid or missing credentials")
                        return@get
                    }
                    logger.debug { "--> /Magic/inspect" }
                    val url = call.request.queryParameters["url"]
                    if (url.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }
                    try {
                        val result: Inspection = Api.inspect(url)
                        logger.debug { "result.container: ${result.container}" }
                        call.respond<Inspection>(result)
                    } catch (t: Throwable) {
                        logger.error(t) {}
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }

                sse("/download") {
                    if (!call.verifyPassword(_password.value)) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid or missing credentials")
                        return@sse
                    }
                    val url = call.request.queryParameters["url"]
                    if (url.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@sse
                    }
                    try {
                        Api.download(url)
                        send(ServerSentEvent(event = "completed"))
                    } catch (t: Throwable) {
                        logger.error(t) {}
                        send(ServerSentEvent(event = "error"))
                    }
                    close()
                }

                get("/getResult") {
                    if (!call.verifyPassword(_password.value)) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid or missing credentials")
                        return@get
                    }
                    val file: File = Api.getResult()
                    call.response.header(
                        name = HttpHeaders.ContentDisposition,
                        value = "attachment; filename=\"${file.name}\""
                    )
                    call.respondFile(file)
                }
            }
        }
    }
}