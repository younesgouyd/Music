package dev.younesgouyd.apps.music.server.multiplatform

import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.Inspection
import dev.younesgouyd.apps.music.common.sha256Hash
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
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import java.io.InputStream
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.io.encoding.Base64

class YtDlp(
    serverHost: String,
    serverFingerprint: String,
    password: String
) {
    private val client = HttpClient(OkHttp) {
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
                host = serverHost
                port = 8443
                path("Magic/")
            }
            header(HttpHeaders.Authorization, Base64.encode(password.encodeToByteArray()))
        }
    }

    suspend fun inspect(url: String): Inspection {
        return client.get("inspect") {
            parameter("url", url)
        }.body<Inspection>()
    }

    fun download(url: String): Flow<String> {
        return flow {
            client.sse(
                urlString = "download",
                request = { parameter("url", url) }
            ) {
                incoming.collect {
                    this@flow.emit(it.event!!)
                }
            }
        }
    }

    suspend fun getResult(): Pair<String, InputStream> {
        val response = client.get("getResult")
        val stream = response.bodyAsChannel().toInputStream()
        val filename = response.headers[HttpHeaders.ContentDisposition]!!
            .substringAfter("filename=\"")
            .substringBeforeLast("\"")
        return filename to stream
    }

    suspend fun close() {
        client.close()
        client.coroutineContext.job.join()
    }
}
