package dev.younesgouyd.apps.music.server.multiplatform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import dev.younesgouyd.apps.music.common.ServerState
import dev.younesgouyd.apps.music.common.getLocalIpAddress
import dev.younesgouyd.apps.music.common.models.ServerInfo
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

@Composable
fun Ui(
    serverStatus: StateFlow<ServerState>,
    certificateFingerprint: StateFlow<String>,
    password: StateFlow<String>,
    onStartClick: (YtDlpServerInfo) -> Unit,
    onStopClick: () -> Unit
) {
    val serverStatus by serverStatus.collectAsState()
    val ipAddress by produceState("") { value = getLocalIpAddress() }
    val certificateFingerprint by certificateFingerprint.collectAsState()
    val password by password.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    val base64Json = remember(ipAddress, certificateFingerprint, password) {
        Base64.encode(
            Json.encodeToString(
                ServerInfo(ipAddress, certificateFingerprint, password)
            ).encodeToByteArray()
        )
    }
    var ytDlpServerInfo by remember { mutableStateOf(YtDlpServerInfo.Empty) }

    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterVertically)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("server status: ")
                    Text(serverStatus.toString())
                }
                OutlinedTextField(
                    label = { Text("ip address") },
                    value = ipAddress,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true
                )
                OutlinedTextField(
                    label = { Text("certificate fingerprint") },
                    value = certificateFingerprint,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true
                )
                OutlinedTextField(
                    label = { Text("password") },
                    value = password,
                    onValueChange = {},
                    readOnly = true,
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
                    value = base64Json,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true
                )
                when (serverStatus) {
                    ServerState.Running -> Button(onStopClick) { Text("stop server") }
                    ServerState.Stopped -> Button({ onStartClick(ytDlpServerInfo) }) { Text("start server") }
                    else -> {}
                }
                YtDlp(
                    state = ytDlpServerInfo,
                    onStateChange = { ytDlpServerInfo = it }
                )
            }
        }
    }
}

@Composable
private fun YtDlp(
    state: YtDlpServerInfo,
    onStateChange: (YtDlpServerInfo) -> Unit
) {
    var ipAddress by remember { mutableStateOf(state.ipAddress) }
    var fingerprint by remember { mutableStateOf(state.serverFingerprint) }
    var password by remember { mutableStateOf(state.password) }
    var base64 by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var base64Error by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
//        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 8.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "YtDlp Server Address",
                style = MaterialTheme.typography.headlineMedium
            )
            OutlinedTextField(
                label = { Text("server ip address") },
                value = ipAddress,
                onValueChange = {
                    ipAddress = it
                    base64 = ""
                    onStateChange(YtDlpServerInfo(ipAddress, fingerprint, password))
                },
                singleLine = true
            )
            OutlinedTextField(
                label = { Text("server fingerprint") },
                value = fingerprint,
                onValueChange = {
                    fingerprint = it
                    base64 = ""
                    onStateChange(YtDlpServerInfo(ipAddress, fingerprint, password))
                },
                singleLine = true
            )
            OutlinedTextField(
                label = { Text("password") },
                value = password,
                onValueChange = {
                    password = it
                    base64 = ""
                    onStateChange(YtDlpServerInfo(ipAddress, fingerprint, password))
                },
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
                        onStateChange(YtDlpServerInfo(ipAddress, fingerprint, password))
                        base64Error = false
                    } catch (_: Exception) {
                        base64Error = true
                    }
                },
                singleLine = true,
                isError = base64Error
            )
        }
    }
}
