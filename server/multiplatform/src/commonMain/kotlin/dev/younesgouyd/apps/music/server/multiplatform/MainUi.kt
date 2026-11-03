package dev.younesgouyd.apps.music.server.multiplatform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.ClientSessionId
import dev.younesgouyd.apps.music.common.models.DeviceType
import dev.younesgouyd.apps.music.common.models.datamodels.ServerInfo
import dev.younesgouyd.apps.music.common.models.datamodels.YtDlpServerInfo
import dev.younesgouyd.apps.music.common.ui.Component
import dev.younesgouyd.apps.music.common.ui.DeviceIcon
import dev.younesgouyd.apps.music.common.ui.Item
import dev.younesgouyd.apps.music.common.ui.YounesMusicTheme
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.ClientRepo
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.ClientSessionRepo
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.SettingRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MainUi(
    private val certificateFingerprint: String,
    private val password: String,
    private val clientRepo: ClientRepo,
    private val clientSessionRepo: ClientSessionRepo,
    private val settingRepo: SettingRepo
) : Component() {
    override val title: String get() = ""
    private val state: MutableStateFlow<Ui.State> = MutableStateFlow(Ui.State.Loading)

    init {
        coroutineScope.launch {
            state.value = Ui.State.Loaded(
                clients = clientRepo.getAll().mapLatest {
                    val datetimeFormat = DateTimeComponents.Format {
                        day(); char(' '); monthName(MonthNames.ENGLISH_ABBREVIATED); char(' '); year(); chars(", "); hour(); char(':'); minute()
                    }
                    it.map {
                        Ui.State.Loaded.Client(
                            id = it.id,
                            name = it.name,
                            deviceType = it.deviceType,
                            sessions = clientSessionRepo.getLast50(it.id).mapLatest {
                                it.map {
                                    Ui.State.Loaded.Client.ClientSession(
                                        id = it.id,
                                        connectionDatetime = Instant.fromEpochMilliseconds(it.connectionDatetime).format(datetimeFormat),
                                        disconnectionDatetime = it.disconnectionDatetime?.let { Instant.fromEpochMilliseconds(it).format(datetimeFormat) },
                                        isDisconnected = it.disconnectionFlag
                                    )
                                }
                            }
                        )
                    }
                }.stateIn(coroutineScope),
                certificateFingerprint = certificateFingerprint,
                password = password,
                ytDlpServerInfo = settingRepo.get(SettingRepo.SettingName.YT_DLP_SERVER_INFO)
                    .mapLatest { setting ->
                        setting?.let { Json.decodeFromString<YtDlpServerInfo>(it.value) }
                    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null),
                onUpdateYtDlpServerInfo = { info ->
                    coroutineScope.launch {
                        settingRepo.set(SettingRepo.SettingName.YT_DLP_SERVER_INFO, Json.encodeToString(info))
                    }
                },
                onUpdateClient = { id: ClientId, name: String, deviceType: DeviceType? ->
                    coroutineScope.launch {
                        clientRepo.update(id, name, deviceType)
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

    private object Ui {
        sealed class State {
            data object Loading : State()

            data class Loaded(
                val clients: StateFlow<List<Client>>,
                val certificateFingerprint: String,
                val password: String,
                val ytDlpServerInfo: StateFlow<YtDlpServerInfo?>,
                val onUpdateYtDlpServerInfo: (YtDlpServerInfo) -> Unit,
                val onUpdateClient: (id: ClientId, name: String, deviceType: DeviceType?) -> Unit
            ) : State() {
                data class Client(
                    val id: ClientId,
                    val name: String,
                    val deviceType: DeviceType?,
                    val sessions: Flow<List<ClientSession>>
                ) {
                    data class ClientSession(
                        val id: ClientSessionId,
                        val connectionDatetime: String,
                        val disconnectionDatetime: String?,
                        val isDisconnected: Boolean
                    )
                }
            }
        }

        @Composable
        fun Main(state: State) {
            YounesMusicTheme {
                when (state) {
                    is State.Loaded -> Main(state)
                    is State.Loading -> Text("Loading...")
                }
            }
        }

        @Composable
        private fun Main(state: State.Loaded) {
            Main(
                clients = state.clients,
                certificateFingerprint = state.certificateFingerprint,
                password = state.password,
                ytDlpServerInfo = state.ytDlpServerInfo,
                onUpdateYtDlpServerInfo = state.onUpdateYtDlpServerInfo,
                onUpdateClient = state.onUpdateClient
            )
        }

        private enum class Screens(
            val label: String,
            val icon: ImageVector
        ) {
            ServerConnectionDetails("Connection Details", Icons.Default.Domain),
            Clients("Clients", Icons.Default.Devices)
        }

        @Composable
        private fun Main(
            clients: StateFlow<List<State.Loaded.Client>>,
            certificateFingerprint: String,
            password: String,
            ytDlpServerInfo: StateFlow<YtDlpServerInfo?>,
            onUpdateYtDlpServerInfo: (YtDlpServerInfo) -> Unit,
            onUpdateClient: (id: ClientId, name: String, deviceType: DeviceType?) -> Unit
        ) {
            var selectedScreen by remember { mutableStateOf(Screens.ServerConnectionDetails) }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                content = { padding ->
                    when (selectedScreen) {
                        Screens.ServerConnectionDetails -> {
                            ServerConnectionDetails(
                                modifier = Modifier.padding(padding),
                                certificateFingerprint = certificateFingerprint,
                                password = password,
                                ytDlpServerInfo = ytDlpServerInfo,
                                onUpdateYtDlpServerInfo = onUpdateYtDlpServerInfo
                            )
                        }
                        Screens.Clients -> {
                            Clients(
                                modifier = Modifier.padding(padding),
                                clients = clients,
                                onUpdateClient = onUpdateClient
                            )
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                        for (screen in Screens.entries) {
                            NavigationBarItem(
                                selected = selectedScreen == screen,
                                onClick = { selectedScreen = screen },
                                icon = { Icon(screen.icon, null) },
                                label = { Text(screen.label) }
                            )
                        }
                    }
                }
            )
        }

        @Composable
        private fun ServerConnectionDetails(
            modifier: Modifier,
            certificateFingerprint: String,
            password: String,
            ytDlpServerInfo: StateFlow<YtDlpServerInfo?>,
            onUpdateYtDlpServerInfo: (YtDlpServerInfo) -> Unit
        ) {
            var passwordVisible by remember { mutableStateOf(false) }
            val json = remember(certificateFingerprint, password) {
                Json.encodeToString(
                    ServerInfo(certificateFingerprint, password)
                )
            }

            Column(
                modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterVertically)
            ) {
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
                    label = { Text("JSON") },
                    value = json,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true
                )
                YtDlp(
                    state = ytDlpServerInfo,
                    onUpdateYtDlpServerInfo = onUpdateYtDlpServerInfo
                )
            }
        }

        @Composable
        private fun YtDlp(
            state: StateFlow<YtDlpServerInfo?>,
            onUpdateYtDlpServerInfo: (YtDlpServerInfo) -> Unit
        ) {
            val state by state.collectAsState()
            var ipAddress by remember { mutableStateOf(state?.ipAddress ?: "") }
            var fingerprint by remember { mutableStateOf(state?.serverFingerprint ?: "") }
            var password by remember { mutableStateOf(state?.password ?: "") }
            var base64 by remember { mutableStateOf("") }
            var passwordVisible by remember { mutableStateOf(false) }
            var base64Error by remember { mutableStateOf(false) }

            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 8.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "YtDlp Server Connection Details",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    OutlinedTextField(
                        label = { Text("server ip address") },
                        value = ipAddress,
                        onValueChange = {
                            ipAddress = it
                            base64 = ""

                        },
                        singleLine = true
                    )
                    OutlinedTextField(
                        label = { Text("server fingerprint") },
                        value = fingerprint,
                        onValueChange = {
                            fingerprint = it
                            base64 = ""
                        },
                        singleLine = true
                    )
                    OutlinedTextField(
                        label = { Text("password") },
                        value = password,
                        onValueChange = {
                            password = it
                            base64 = ""
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
                                val some: YtDlpServerInfo = Json.decodeFromString(
                                    Base64.decode(it).decodeToString()
                                )
                                ipAddress = some.ipAddress
                                fingerprint = some.serverFingerprint
                                password = some.password
                                base64Error = false
                            } catch (_: Exception) {
                                base64Error = true
                            }
                        },
                        singleLine = true,
                        isError = base64Error
                    )
                    OutlinedButton(
                        onClick = { onUpdateYtDlpServerInfo(YtDlpServerInfo(ipAddress, fingerprint, password)) },
                        content = { Text("Set") }
                    )
                }
            }
        }

        @Composable
        private fun Clients(
            modifier: Modifier,
            clients: StateFlow<List<State.Loaded.Client>>,
            onUpdateClient: (id: ClientId, name: String, deviceType: DeviceType?) -> Unit
        ) {
            val clients by clients.collectAsState()
            var showSessions by remember { mutableStateOf<State.Loaded.Client?>(null) }

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(
                    items = clients,
                    key = { it.id.value }
                ) { client ->
                    Item(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(8.dp),
                        onClick = { showSessions = client }
                    ) {
                        Client(
                            client = client,
                            onUpdate = { name: String, deviceType: DeviceType? ->
                                onUpdateClient(client.id, name, deviceType)
                            }
                        )
                    }
                }
            }

            showSessions?.let { client ->
                Dialog(onDismissRequest = { showSessions = null }) {
                    Sessions(
                        client = client,
                        onUpdate = { name: String, deviceType: DeviceType? ->
                            onUpdateClient(client.id, name, deviceType)
                        }
                    )
                }
            }
        }

        @Composable
        fun ConnectionIndicator(connected: Boolean) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = if (connected) Color.Green else Color.Red,
                        shape = CircleShape,
                    )
            )
        }

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        private fun Client(
            client: State.Loaded.Client,
            onUpdate: (name: String, deviceType: DeviceType?) -> Unit
        ) {
            val sessions by client.sessions.collectAsState(emptyList())
            var showEditDialog by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConnectionIndicator(sessions.firstOrNull()?.isDisconnected == false)
                DeviceIcon(client.deviceType)
                Text(
                    modifier = Modifier.weight(1f),
                    text = "${client.id} - ${client.name}",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(
                    onClick = { showEditDialog = true },
                    content = { Icon(Icons.Default.Edit, null) }
                )
            }

            fun dismissDialog() {
                showEditDialog = false
            }

            if (showEditDialog) {
                Dialog(onDismissRequest = ::dismissDialog) {
                    var name by remember { mutableStateOf(client.name) }
                    var deviceType by remember { mutableStateOf(client.deviceType) }
                    var expanded by remember { mutableStateOf(false) }

                    Surface(
                        modifier = Modifier.width(500.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("name") },
                                value = name,
                                onValueChange = { name = it }
                            )
                            ExposedDropdownMenuBox(
                                modifier = Modifier.fillMaxWidth(),
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                    label = { Text("Device Type") },
                                    leadingIcon = { DeviceIcon(deviceType) },
                                    value = deviceType?.toString() ?: "",
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
                                    for (entry in DeviceType.entries) {
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    DeviceIcon(entry)
                                                    Text(entry.toString())
                                                }
                                            },
                                            onClick = {
                                                deviceType = entry
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onUpdate(name, deviceType); dismissDialog() },
                                content = { Text("Done") }
                            )
                        }
                    }
                }
            }
        }

        @Composable
        private fun Sessions(
            client: State.Loaded.Client,
            onUpdate: (name: String, deviceType: DeviceType?) -> Unit
        ) {
            val sessions by client.sessions.collectAsState(emptyList())

            Surface(
                modifier = Modifier.size(width = 500.dp, height = 600.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Client(
                        client = client,
                        onUpdate = onUpdate
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(
                            items = sessions,
                            key = { it.id.value }
                        ) { session ->
                            Surface(Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = session.connectionDatetime,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Icon(Icons.AutoMirrored.Default.ArrowRightAlt, null)
                                    Text(
                                        text = session.disconnectionDatetime ?: "",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
