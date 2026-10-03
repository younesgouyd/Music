package dev.younesgouyd.apps.music.ytdlp

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
import dev.younesgouyd.apps.music.common.getLocalInetAddress
import dev.younesgouyd.apps.music.common.models.ServerInfo
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

@Composable
fun Ui(
    serverStatus: StateFlow<ServerState>,
    certificateSignature: StateFlow<String>,
    password: StateFlow<String>,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val serverStatus by serverStatus.collectAsState()
    val ipAddress by produceState("") { value = getLocalInetAddress()?.hostAddress ?: "" }
    val certificateSignature by certificateSignature.collectAsState()
    val password by password.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    val base64Json = remember(ipAddress, certificateSignature, password) {
        Base64.encode(
            Json.encodeToString(
                ServerInfo(ipAddress, certificateSignature, password)
            ).encodeToByteArray()
        )
    }

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
                    value = certificateSignature,
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
                    ServerState.Stopped -> Button(onStartClick) { Text("start server") }
                    else -> {}
                }
            }
        }
    }
}
