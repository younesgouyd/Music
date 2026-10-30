package dev.younesgouyd.apps.music.server.multiplatform

import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.datamodels.Inspection
import dev.younesgouyd.apps.music.common.models.datamodels.YtDlpServerInfo
import dev.younesgouyd.apps.music.common.sha256Hash
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.SettingRepo
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import okhttp3.internal.closeQuietly
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.io.encoding.Base64

class YtDlp(
    settingRepo: SettingRepo
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val client: StateFlow<HttpClient?> =
        settingRepo.get(SettingRepo.SettingName.YT_DLP_SERVER_INFO)
            .transformLatest { setting ->
                val info: YtDlpServerInfo? = setting?.value?.let { Json.decodeFromString(it) }
                if (info == null) {
                    emit(null)
                } else {
                    val httpClient = HttpClient(OkHttp) {
                        engine {
                            config {
                                val trustManager: X509TrustManager = TrustManager(info.serverFingerprint)
                                val sslContext = SSLContext.getInstance("TLSv1.3").apply {
                                    init(null, arrayOf(trustManager), SecureRandom())
                                }
                                sslSocketFactory(sslContext.socketFactory, trustManager)
                                hostnameVerifier { _, _ -> true }
                            }
                        }
                        install(Logging) { level = LogLevel.HEADERS }
                        install(ContentNegotiation) { json(json) }
                        install(SSE)
                        install(HttpTimeout) {
                            requestTimeoutMillis = 5*60*1000
                            socketTimeoutMillis = 5*60*1000
                        }
                        defaultRequest {
                            url {
                                protocol = URLProtocol.HTTPS
                                host = info.ipAddress
                                port = 8443
                                path("Magic/")
                            }
                            header(HttpHeaders.Authorization, Base64.encode(info.password.encodeToByteArray()))
                        }
                    }
                    try {
                        emit(httpClient)
                        awaitCancellation()
                    } finally {
                        withContext(NonCancellable) {
                            httpClient.closeQuietly()
                            httpClient.coroutineContext.job.join()
                        }
                    }
                }
            }.stateIn(coroutineScope, SharingStarted.Eagerly, null)

    suspend fun inspect(url: String): Inspection {
        return client.value!!.get("inspect") {
            parameter("url", url)
        }.body<Inspection>()
    }

    fun download(url: String): Flow<String> {
        return flow {
            client.value!!.sse(
                urlString = "download",
                request = { parameter("url", url) }
            ) {
                incoming.collect {
                    this@flow.emit(it.event!!)
                }
            }
        }
    }

    suspend fun <T> getResult(handleChannel: suspend (filename: String, channel: ByteReadChannel) -> T): T {
        return client.value!!.prepareGet("getResult").execute { response ->
            handleChannel(
                response.headers[HttpHeaders.ContentDisposition]!!
                    .substringAfter("filename=\"")
                    .substringBeforeLast("\""),
                response.bodyAsChannel()
            )
        }
    }

    suspend fun close() {
        val client = client.value
        if (client != null) {
            client.closeQuietly()
            client.coroutineContext.job.join()
        }
    }

    @Suppress("CustomX509TrustManager")
    private class TrustManager(
        private val certificateFingerprint: String
    ) : X509TrustManager {
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
            if (!actualFingerprint.equals(certificateFingerprint, ignoreCase = true)) {
                throw CertificateException("Certificate fingerprint mismatch! Expected: ${certificateFingerprint}, Got: $actualFingerprint")
            }
        }
    }
}
