package dev.younesgouyd.apps.music.client.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.common.data.repoes.SettingsRepo
import dev.younesgouyd.apps.music.client.common.data.repoes.SpotifyAuthRepo
import dev.younesgouyd.apps.music.client.common.util.Component
import dev.younesgouyd.apps.music.common.DarkThemeOptions
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class Settings(
    private val settingsRepo: SettingsRepo,
    private val spotifyAuthRepo: SpotifyAuthRepo
) : Component() {
    override val title: String = "Settings"
    private val spotifyState: MutableStateFlow<SpotifyState> = MutableStateFlow(SpotifyState.Loading)
    private val clientId = MutableStateFlow("")
    private val clientSecret = MutableStateFlow("")
    private val spotifyUiEnabled = MutableStateFlow(false)
    private val state: MutableStateFlow<Ui.State> = MutableStateFlow(Ui.State.Loading)

    init {
        coroutineScope.launch {
            state.value = Ui.State.Loaded(
                darkTheme = settingsRepo.getDarkTheme().map { DarkThemeOptions.valueOf(it.value) }.stateIn(coroutineScope),
                spotifyState = spotifyState.asStateFlow(),
                onDarkThemeChange = ::updateDarkTheme
            )
        }
        refreshSpotifyState()
    }

    private fun refreshSpotifyState() {
        coroutineScope.launch {
            val spotifyAuthState = spotifyAuthRepo.getAuthState()
            clientId.value = spotifyAuthState.clientId ?: ""
            clientSecret.value = spotifyAuthState.clientSecret ?: ""
            spotifyState.value = if (spotifyAuthState.isAuthorized) {
                SpotifyState.Authorized(
                    clientId = clientId.asStateFlow(),
                    clientSecret = clientSecret.asStateFlow(),
                    inputUiEnabled = spotifyUiEnabled.asStateFlow(),
                    onClientIdChange = { clientId.value = it },
                    onClientSecretChange = { clientSecret.value = it },
                    onUpdateCredentials = ::connectToSpotify,
                    onClearCredentialsClick = {
                        coroutineScope.launch {
                            spotifyAuthRepo.deauthorize()
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
            spotifyAuthRepo.updateCredentials(clientId, clientSecret)
            spotifyAuthRepo.authorize()
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

    private object Ui {
        sealed class State {
            data object Loading : State()

            data class Loaded(
                val darkTheme: StateFlow<DarkThemeOptions?>,
                val spotifyState: StateFlow<SpotifyState>,
                val onDarkThemeChange: (DarkThemeOptions) -> Unit
            ) : State()
        }

        @Composable
        fun Main(modifier: Modifier, state: State) {
            when (state) {
                is State.Loading -> Text(modifier = modifier, text = "Loading...")
                is State.Loaded -> Settings(modifier = modifier, loaded = state)
            }
        }

        @Composable
        private fun Settings(modifier: Modifier, loaded: State.Loaded) {
            val scrollState = rememberScrollState()
            val darkTheme by loaded.darkTheme.collectAsState()
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
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        value = selectedOption?.label ?: "",
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
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