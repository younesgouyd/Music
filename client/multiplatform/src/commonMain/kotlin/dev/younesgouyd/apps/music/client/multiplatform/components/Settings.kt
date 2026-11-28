package dev.younesgouyd.apps.music.client.multiplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.SettingRepo
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.SpotifyAuthRepo
import dev.younesgouyd.apps.music.common.models.datamodels.Setting
import dev.younesgouyd.apps.music.common.ui.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class Settings(
    private val settingRepo: SettingRepo,
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
                darkTheme = settingRepo.get(Setting.DarkMode.NAME)
                    .map { (it as Setting.DarkMode).value }
                    .stateIn(coroutineScope),
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
            spotifyAuthRepo.authorize(clientId, clientSecret)
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

    private fun updateDarkTheme(newValue: Setting.DarkMode.Values) {
        coroutineScope.launch {
            settingRepo.set(Setting.DarkMode(newValue))
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

            class Loaded(
                val darkTheme: StateFlow<Setting.DarkMode.Values>,
                val spotifyState: StateFlow<SpotifyState>,
                val onDarkThemeChange: (Setting.DarkMode.Values) -> Unit
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
            val darkTheme = loaded.darkTheme
            val spotifyState by loaded.spotifyState.collectAsState()

            Box(
                modifier = modifier.fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = modifier.width(500.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DarkModeField(
                        modifier = Modifier.fillMaxWidth(),
                        value = darkTheme,
                        onValueChange = loaded.onDarkThemeChange
                    )
                    Spotify(
                        modifier = Modifier.fillMaxWidth(),
                        state = spotifyState
                    )
                }
            }
        }

        @Composable
        private fun DarkModeField(
            modifier: Modifier,
            value: StateFlow<Setting.DarkMode.Values>,
            onValueChange: (Setting.DarkMode.Values) -> Unit
        ) {
            val value by value.collectAsState()
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                modifier = modifier,
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    label = { Text("Dark Mode") },
                    value = value.label,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = MenuDefaults.groupStandardContainerColor,
                    shape = MenuDefaults.standaloneGroupShape
                ) {
                    Setting.DarkMode.Values.entries.forEachIndexed { index, option ->
                        SelectableDropdownMenuItem(
                            text = { Text(option.label) },
                            selected = option == value,
                            onClick = { onValueChange(option) },
                            shapes = MenuDefaults.itemShape(index, Setting.DarkMode.Values.entries.size),
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
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