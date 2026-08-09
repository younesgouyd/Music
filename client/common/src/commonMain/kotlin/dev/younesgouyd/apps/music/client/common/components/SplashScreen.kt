package dev.younesgouyd.apps.music.client.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.common.util.Component
import dev.younesgouyd.apps.music.common.json
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SplashScreen(
    val onStart: (host: String, port: Int) -> Unit,
    val loading: StateFlow<Boolean>
) : Component() {
    override val title: String = ""
    private val logger = KotlinLogging.logger {  }
    private val client = HttpClient(CIO) {
        install(Logging) { level = LogLevel.ALL }
        install(ContentNegotiation) { json(json) }
    }
    private val error: MutableStateFlow<Boolean> = MutableStateFlow(false)

    @Composable
    override fun show(modifier: Modifier) {
        var host by remember { mutableStateOf("localhost") }
        var port by remember { mutableStateOf("8080") }
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
                            OutlinedTextField(
                                label = { Text("Port") },
                                value = port,
                                onValueChange = { port = it },
                                singleLine = true,
                                isError = port.toIntOrNull() == null
                            )
                            Button(
                                onClick = { setAddress(host, port.toInt()) }
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

    private fun setAddress(host: String, port: Int) {
        coroutineScope.launch {
            val result = try {
                client.request {
                    url {
                        this.protocol = URLProtocol.HTTP
                        this.host = host
                        this.port = port
                    }
                }.bodyAsText()
            } catch (e: Exception) {
                logger.error(e) {}
                null
            }
            if (result == "music backend") {
                onStart(host, port)
            } else {
                error.value = true
            }
        }
    }
}