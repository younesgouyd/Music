package dev.younesgouyd.apps.music.client.multiplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.multiplatform.appDir
import dev.younesgouyd.apps.music.client.multiplatform.createHttpClient
import dev.younesgouyd.apps.music.client.multiplatform.util.Component
import dev.younesgouyd.apps.music.common.models.ServerInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.io.encoding.Base64

class StartupScreen(
    val onStart: (
        serverIpAddr: String,
        serverFingerprint: String,
        password: String
    ) -> Unit,
    val loading: StateFlow<Boolean>
) : Component() {
    override val title: String = ""
    private val logger = KotlinLogging.logger {  }
    private val state: MutableStateFlow<Ui.State> = MutableStateFlow(Ui.State.Loading)
    private val error: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val testingConnection: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var tries = 0

    private val serverInfoDir = File(appDir, "server_info")
    private val ipAddressFile = File(serverInfoDir, "ip_address.txt")
    private val fingerprintFile = File(serverInfoDir, "fingerprint.txt")
    private val passwordFile = File(serverInfoDir, "password.txt")

    init {
        coroutineScope.launch(Dispatchers.IO) {
            appDir.mkdir()
            serverInfoDir.mkdir()
            if (!ipAddressFile.exists()) ipAddressFile.createNewFile()
            if (!fingerprintFile.exists()) fingerprintFile.createNewFile()
            if (!passwordFile.exists()) passwordFile.createNewFile()
            val ipAddr = ipAddressFile.readText()
            val fng = fingerprintFile.readText()
            val pass = passwordFile.readText()
            if (ipAddr.isNotBlank() && fng.isNotBlank() && pass.isNotBlank()) {
                tryConnect(ipAddr, fng, pass)
            }
            tries++
            state.value = Ui.State.Loaded(
                serverIpAddress = ipAddressFile.readText(),
                serverFingerprint = fingerprintFile.readText(),
                serverPassword = passwordFile.readText(),
                error = error.asStateFlow(),
                testingConnection = testingConnection.asStateFlow(),
                onStartClick = { serverIpAddr: String, serverFingerprint: String, password: String ->
                    coroutineScope.launch {
                        tryConnect(serverIpAddr, serverFingerprint, password)
                    }
                }
            )
        }
    }

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private suspend fun tryConnect(
        serverIpAddr: String,
        serverFingerprint: String,
        password: String
    ) {
        testingConnection.value = true
        val client = createHttpClient(
            serverHost = serverIpAddr,
            serverFingerprint = serverFingerprint,
            password = password
        )
        withContext(Dispatchers.IO) {
            val result = try {
                client.request("/") {
                    header(HttpHeaders.Authorization, Base64.encode(password.encodeToByteArray()))
                }.bodyAsText()
            } catch (e: Exception) {
                logger.error(e) {}
                null
            }
            if (result == "music server") {
                client.close()
                client.coroutineContext.job.join()
                ipAddressFile.writeText(serverIpAddr)
                fingerprintFile.writeText(serverFingerprint)
                passwordFile.writeText(password)
                state.value = Ui.State.Loading
                onStart(serverIpAddr, serverFingerprint, password)
            } else {
                error.value = tries > 0
            }
        }
        testingConnection.value = false
    }

    private object Ui {
        sealed class State {
            data object Loading : State()
            data class Loaded(
                val serverIpAddress: String,
                val serverFingerprint: String,
                val serverPassword: String,
                val error: StateFlow<Boolean>,
                val testingConnection: StateFlow<Boolean>,
                val onStartClick: (
                    serverIpAddr: String,
                    serverFingerprint: String,
                    password: String
                ) -> Unit
            ) : State()
        }

        @Composable
        fun Main(state: State) {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (state) {
                            is State.Loading -> Text("Loading...")
                            is State.Loaded -> Main(state)
                        }
                    }
                }
            }
        }

        @Composable
        private fun Main(state: State.Loaded) {
            var ipAddress by remember { mutableStateOf(state.serverIpAddress) }
            var fingerprint by remember { mutableStateOf(state.serverFingerprint) }
            var password by remember { mutableStateOf(state.serverPassword) }
            var base64 by remember { mutableStateOf("") }
            val testingConnection by state.testingConnection.collectAsState()
            val error by state.error.collectAsState()
            var passwordVisible by remember { mutableStateOf(false) }
            var base64Error by remember { mutableStateOf(false) }

            if (testingConnection) {
                Text("Connecting...")
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        label = { Text("server ip address") },
                        value = ipAddress,
                        onValueChange = { ipAddress = it; base64 = "" },
                        singleLine = true
                    )
                    OutlinedTextField(
                        label = { Text("server fingerprint") },
                        value = fingerprint,
                        onValueChange = { fingerprint = it; base64 = "" },
                        singleLine = true
                    )
                    OutlinedTextField(
                        label = { Text("password") },
                        value = password,
                        onValueChange = { password = it; base64 = "" },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            val description = if (passwordVisible) "Hide password" else "Show password"
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = description)
                            }
                        }
                    )
                    OutlinedTextField(
                        label = { Text("base64") },
                        value = base64,
                        onValueChange = {
                            base64 = it
                            try {
                                val some: ServerInfo = Json.decodeFromString(
                                    Base64.decode(it).decodeToString()
                                )
                                ipAddress = some.ipAddress
                                fingerprint = some.certificateFingerprint
                                password = some.password
                                base64Error = false
                            } catch (_: Exception) {
                                base64Error = true
                            }
                        },
                        singleLine = true,
                        isError = base64Error
                    )
                    Button(
                        onClick = {
                            state.onStartClick(ipAddress, fingerprint, password)
                        }
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