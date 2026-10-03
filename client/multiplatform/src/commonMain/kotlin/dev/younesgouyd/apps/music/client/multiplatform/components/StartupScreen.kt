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
import dev.younesgouyd.apps.music.client.multiplatform.util.Component
import dev.younesgouyd.apps.music.common.models.ServerInfo
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json

class StartupScreen(
    val onConnect: (
        nsdServiceName: String,
        serverFingerprint: String,
        password: String
    ) -> Unit,
    val loading: StateFlow<Boolean>,
    val error: StateFlow<Boolean>
) : Component() {
    override val title: String = ""
    private val state: StateFlow<Ui.State> = loading.map {
        if (it) Ui.State.Loading else Ui.State.Loaded(error, onConnect)
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Ui.State.Loading)

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private object Ui {
        sealed class State {
            data object Loading : State()
            data class Loaded(
                val error: StateFlow<Boolean>,
                val onConnectClick: (
                    nsdServiceName: String,
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
                            is State.Loading -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text("Please wait...")
                                }
                            }
                            is State.Loaded -> Main(state)
                        }
                    }
                }
            }
        }

        @Composable
        private fun Main(state: State.Loaded) {
            var nsdServiceName by remember { mutableStateOf("") }
            var fingerprint by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var json by remember { mutableStateOf("") }
            val error by state.error.collectAsState()
            var passwordVisible by remember { mutableStateOf(false) }
            var jsonError by remember { mutableStateOf(false) }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    label = { Text("NSD service name") },
                    value = nsdServiceName,
                    onValueChange = { nsdServiceName = it; json = "" },
                    singleLine = true
                )
                OutlinedTextField(
                    label = { Text("server fingerprint") },
                    value = fingerprint,
                    onValueChange = { fingerprint = it; json = "" },
                    singleLine = true
                )
                OutlinedTextField(
                    label = { Text("password") },
                    value = password,
                    onValueChange = { password = it; json = "" },
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
                    label = { Text("JSON") },
                    value = json,
                    onValueChange = {
                        json = it
                        try {
                            val some: ServerInfo = Json.decodeFromString(it)
                            nsdServiceName = some.nsdServiceName
                            fingerprint = some.certificateFingerprint
                            password = some.password
                            jsonError = false
                        } catch (_: Exception) {
                            jsonError = true
                        }
                    },
                    singleLine = true,
                    isError = jsonError
                )
                Button(
                    enabled = nsdServiceName.isNotBlank() && fingerprint.isNotBlank() && password.isNotBlank(),
                    onClick = {
                        state.onConnectClick(nsdServiceName, fingerprint, password)
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