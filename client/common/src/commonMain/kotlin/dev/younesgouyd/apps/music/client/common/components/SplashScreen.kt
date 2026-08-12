package dev.younesgouyd.apps.music.client.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.common.util.Component
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class SplashScreen(
    val onStart: (host: String) -> Unit,
    val loading: StateFlow<Boolean>
) : Component() {
    override val title: String = ""
    private val logger = KotlinLogging.logger {  }
    private val client = HttpClient(OkHttp) {
        engine {
            config {
                val trustAllCerts = object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, arrayOf(trustAllCerts), SecureRandom())
                sslSocketFactory(sslContext.socketFactory, trustAllCerts)
                hostnameVerifier { _, _ -> true }
            }
        }
        install(Logging) { level = LogLevel.ALL }
    }
    private val error: MutableStateFlow<Boolean> = MutableStateFlow(false)

    @Composable
    override fun show(modifier: Modifier) {
        var host by remember { mutableStateOf("localhost") }
        val loading by loading.collectAsState()
        val error by error.collectAsState()

        MaterialTheme {
            Surface(
                modifier = modifier,
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (loading) {
                        Text("Loading...")
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                label = { Text("Host") },
                                value = host,
                                onValueChange = { host = it },
                                singleLine = true
                            )
                            Button(
                                onClick = { setAddress(host) }
                            ) {
                                Text("Connect")
                            }
                            if (error) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        modifier = Modifier.padding(8.dp),
                                        text = "Can't connect with server.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun clear() {
        coroutineScope.cancel()
        client.close()
    }

    private fun setAddress(host: String) {
        coroutineScope.launch {
            val result = try {
                client.request {
                    url {
                        this.protocol = URLProtocol.HTTPS
                        this.host = host
                        this.port = 8443
                    }
                }.bodyAsText()
            } catch (e: Exception) {
                logger.error(e) {}
                null
            }
            if (result == "music backend") {
                onStart(host)
            } else {
                error.value = true
            }
        }
    }
}