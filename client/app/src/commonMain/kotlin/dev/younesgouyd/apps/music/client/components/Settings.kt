package dev.younesgouyd.apps.music.client.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.data.repoes.SettingsRepo
import dev.younesgouyd.apps.music.client.util.Component
import dev.younesgouyd.apps.music.client.util.DarkThemeOptions
import dev.younesgouyd.libs.music.spotifyapi.SpotifyApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class Settings(
    private val settingsRepo: SettingsRepo,
    private val spotifyApi: SpotifyApi,
    onReinitializeAppData: () -> Unit
) : Component() {
    override val title: String = "Settings"
    private val spotifyState: MutableStateFlow<SpotifyState> = MutableStateFlow(SpotifyState.Loading)
    private val clientId = MutableStateFlow("")
    private val clientSecret = MutableStateFlow("")
    private val spotifyUiEnabled = MutableStateFlow(false)
    private val state: MutableStateFlow<SettingsState> = MutableStateFlow(SettingsState.Loading)

    init {
        coroutineScope.launch {
            state.update {
                SettingsState.Loaded(
                    darkTheme = settingsRepo.getDarkTheme().map { DarkThemeOptions.valueOf(it.value) }.stateIn(coroutineScope),
                    serverAddress = settingsRepo.getServerAddress().map { it.value }.stateIn(coroutineScope),
                    spotifyState = spotifyState.asStateFlow(),
                    onDarkThemeChange = ::updateDarkTheme,
                    onServerAddressChange = ::updateServerAddress,
                    onReinitializeAppDataClick = onReinitializeAppData
                )
            }
        }
        refreshSpotifyState()
    }

    private fun refreshSpotifyState() {
        coroutineScope.launch {
            clientId.value = settingsRepo.getSpotifyClientId().first().value
            clientSecret.value = settingsRepo.getSpotifyClientSecret().first().value
            spotifyState.value = if (spotifyApi.isAuthorized()) {
                SpotifyState.Authorized(
                    clientId = clientId.asStateFlow(),
                    clientSecret = clientSecret.asStateFlow(),
                    inputUiEnabled = spotifyUiEnabled.asStateFlow(),
                    onClientIdChange = { clientId.value = it },
                    onClientSecretChange = { clientSecret.value = it },
                    onUpdateCredentials = ::connectToSpotify,
                    onClearCredentialsClick = {
                        coroutineScope.launch {
                            spotifyApi.clearToken()
                            settingsRepo.updateSpotifyCredentials("", "")
                            refreshSpotifyState()
                        }
                    }
                )
            } else {
                SpotifyState.Unauthorized(
                    clientId = clientId.asStateFlow(),
                    clientSecret = clientSecret.asStateFlow(),
                    inputUiEnabled = spotifyUiEnabled.asStateFlow(),
                    onClientIdChange = { clientId.value = it },
                    onClientSecretChange = { clientSecret.value = it },
                    onAuthorizeClick = ::connectToSpotify
                )
            }
            spotifyUiEnabled.value = true
        }
    }

    private fun connectToSpotify() {
        coroutineScope.launch {
            spotifyUiEnabled.value = false
            val clientId = clientId.value
            val clientSecret = clientSecret.value
            settingsRepo.updateSpotifyCredentials(clientId, clientSecret)
            spotifyApi.getAuthorization(clientId, clientSecret)
            refreshSpotifyState()
        }
    }

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier = modifier, state = state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private fun updateDarkTheme(newValue: DarkThemeOptions) {
        coroutineScope.launch {
            settingsRepo.updateDarkTheme(newValue)
        }
    }

    private fun updateServerAddress(newValue: String?) {
        coroutineScope.launch {
            settingsRepo.updateServerAddress(newValue)
        }
    }

    sealed class SpotifyState {
        data object Loading : SpotifyState()

        data class Unauthorized(
            val clientId: StateFlow<String>,
            val clientSecret: StateFlow<String>,
            val inputUiEnabled: StateFlow<Boolean>,
            val onClientIdChange: (String) -> Unit,
            val onClientSecretChange: (String) -> Unit,
            val onAuthorizeClick: () -> Unit
        ) : SpotifyState()

        data class Authorized(
            val clientId: StateFlow<String>,
            val clientSecret: StateFlow<String>,
            val inputUiEnabled: StateFlow<Boolean>,
            val onClientIdChange: (String) -> Unit,
            val onClientSecretChange: (String) -> Unit,
            val onUpdateCredentials: () -> Unit,
            val onClearCredentialsClick: () -> Unit
        ) : SpotifyState()
    }

    private sealed class SettingsState {
        data object Loading : SettingsState()

        data class Loaded(
            val darkTheme: StateFlow<DarkThemeOptions?>,
            val serverAddress: StateFlow<String?>,
            val spotifyState: StateFlow<SpotifyState>,
            val onDarkThemeChange: (DarkThemeOptions) -> Unit,
            val onServerAddressChange: (String) -> Unit,
            val onReinitializeAppDataClick: () -> Unit
        ) : SettingsState()
    }

    private object Ui {
        @Composable
        fun Main(modifier: Modifier, state: SettingsState) {
            when (state) {
                is SettingsState.Loading -> Text(modifier = modifier, text = "Loading...")
                is SettingsState.Loaded -> Settings(modifier = modifier, loaded = state)
            }
        }

        @Composable
        private fun Settings(modifier: Modifier, loaded: SettingsState.Loaded) {
            val scrollState = rememberScrollState()
            val darkTheme by loaded.darkTheme.collectAsState()
            val serverAddress by loaded.serverAddress.collectAsState()
            val spotifyState by loaded.spotifyState.collectAsState()

            Column(
                modifier = modifier.fillMaxWidth().verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DarkTheme(
                    modifier = Modifier.fillMaxWidth(),
                    selectedOption = darkTheme,
                    onDarkThemeChange = loaded.onDarkThemeChange
                )
                ServerAddress(
                    modifier = Modifier.fillMaxWidth(),
                    serverAddress = serverAddress,
                    onServerAddressChange = loaded.onServerAddressChange
                )
                ReinitializeAppData(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = loaded.onReinitializeAppDataClick,
                )
                Spotify(
                    modifier = Modifier.fillMaxWidth(),
                    state = spotifyState
                )
            }
        }

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        private fun DarkTheme(
            modifier: Modifier,
            selectedOption: DarkThemeOptions?,
            onDarkThemeChange: (DarkThemeOptions) -> Unit
        ) {
            var expanded by remember { mutableStateOf(false) }

            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark Theme")
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(),
                        value = selectedOption?.label ?: "",
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        for (darkThemeOption in DarkThemeOptions.entries) {
                            DropdownMenuItem(
                                text = { Text(darkThemeOption.label) },
                                onClick = {
                                    onDarkThemeChange(darkThemeOption)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        @Composable
        private fun ServerAddress(
            modifier: Modifier,
            serverAddress: String?,
            onServerAddressChange: (String) -> Unit
        ) {
            OutlinedTextField(
                modifier = modifier,
                value = serverAddress ?: "",
                onValueChange = onServerAddressChange,
                singleLine = true
            )
        }

        @Composable
        private fun ReinitializeAppData(
            modifier: Modifier,
            onClick: () -> Unit
        ) {
            Button(
                modifier = modifier,
                onClick = onClick,
                content = { Text("Reinitialize app data") }
            )
        }

        @Composable
        private fun Spotify(modifier: Modifier, state: SpotifyState) {
            when (state) {
                is SpotifyState.Loading -> Spotify(modifier, state)
                is SpotifyState.Unauthorized -> Spotify(modifier, state)
                is SpotifyState.Authorized -> Spotify(modifier, state)
            }
        }

        @Composable
        private fun Spotify(modifier: Modifier, state: SpotifyState.Loading) {
            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Spotify Service",
                        style = MaterialTheme.typography.titleMedium
                    )
                    CircularProgressIndicator()
                }
            }
        }

        @Composable
        private fun Spotify(modifier: Modifier, state: SpotifyState.Unauthorized) {
            val clientId by state.clientId.collectAsState()
            val clientSecret by state.clientSecret.collectAsState()
            val inputUiEnabled by state.inputUiEnabled.collectAsState()
            val onClientIdChange: (String) -> Unit = state.onClientIdChange
            val onClientSecretChange: (String) -> Unit = state.onClientSecretChange
            val onAuthorizeClick: () -> Unit = state.onAuthorizeClick

            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Spotify Service (Unauthorized)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        label = { Text("Client ID") },
                        value = clientId,
                        onValueChange = { onClientIdChange(it) },
                        enabled = inputUiEnabled
                    )
                    OutlinedTextField(
                        label = { Text("Client Secret") },
                        value = clientSecret,
                        onValueChange = { onClientSecretChange(it) },
                        enabled = inputUiEnabled
                    )
                    Button(
                        content = { Text("Authorize") },
                        onClick = onAuthorizeClick,
                        enabled = inputUiEnabled && clientId.isNotBlank() && clientSecret.isNotBlank()
                    )
                }
            }
        }

        @Composable
        private fun Spotify(modifier: Modifier, state: SpotifyState.Authorized) {
            val clientId by state.clientId.collectAsState()
            val clientSecret by state.clientSecret.collectAsState()
            val inputUiEnabled by state.inputUiEnabled.collectAsState()
            val onClientIdChange: (String) -> Unit = state.onClientIdChange
            val onClientSecretChange: (String) -> Unit = state.onClientSecretChange
            val onUpdateCredentials: () -> Unit = state.onUpdateCredentials
            val onClearCredentialsClick: () -> Unit = state.onClearCredentialsClick

            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Spotify Service (Authorized)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        label = { Text("Client ID") },
                        value = clientId,
                        onValueChange = { onClientIdChange(it) },
                        enabled = inputUiEnabled
                    )
                    OutlinedTextField(
                        label = { Text("Client Secret") },
                        value = clientSecret,
                        onValueChange = { onClientSecretChange(it) },
                        enabled = inputUiEnabled
                    )
                    Button(
                        content = { Text("Update Credentials") },
                        onClick = onUpdateCredentials,
                        enabled = inputUiEnabled && clientId.isNotBlank() && clientSecret.isNotBlank()
                    )
                    Button(
                        content = { Text("Clear Credentials") },
                        onClick = onClearCredentialsClick,
                        enabled = inputUiEnabled
                    )
                }
            }
        }
    }
}