package dev.younesgouyd.apps.music.client.multiplatform

import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.sha256Hash
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.io.encoding.Base64

fun createHttpClient(
    serverHost: String,
    serverFingerprint: String,
    password: String
): HttpClient {
    return HttpClient(OkHttp) {
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
        install(Logging) { level = LogLevel.ALL }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(json)
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
}