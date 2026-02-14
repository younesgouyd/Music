package dev.younesgouyd.apps.music.client.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.components.util.Image
import dev.younesgouyd.apps.music.client.components.util.Item
import dev.younesgouyd.apps.music.client.components.util.formatted
import dev.younesgouyd.apps.music.client.data.FileManager
import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.ImportSessionId
import dev.younesgouyd.apps.music.client.data.Server
import dev.younesgouyd.apps.music.client.data.repoes.ImportSessionItemRepo
import dev.younesgouyd.apps.music.client.data.repoes.ImportSessionRepo
import dev.younesgouyd.apps.music.client.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSession
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.client.util.Component
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.milliseconds

class ImportFromInternetFlow(
    destinationFolderId: FolderId,
    importSessionRepo: ImportSessionRepo,
    importSessionItemRepo: ImportSessionItemRepo,
    mediaFileRepo: MediaFileRepo,
    server: Server,
    fileManager: FileManager,
    showImportSession: (ImportSessionId, ImportSessionItem.State) -> Unit
) : Component() {
    override val title: String = "Inspection"
    private val state: Ui.State

    init {
        val inspecting = MutableStateFlow(false)
        val savingImport = MutableStateFlow(false)
        val inspectionError = MutableStateFlow(false)
        val url = MutableStateFlow("")
        val inspection = MutableStateFlow<Inspection.Webpage?>(null)
        val selectedItems = MutableStateFlow<List<Long>>(emptyList())
        state = Ui.State(
            inspecting = inspecting.asStateFlow(),
            savingImport = savingImport.asStateFlow(),
            inspectionError = inspectionError.asStateFlow(),
            url = url.asStateFlow(),
            inspection = inspection.asStateFlow(),
            selectedItems = selectedItems.asStateFlow(),
            onUrlChange = {
                inspectionError.value = false
                url.value = it
            },
            onInspectClick = {
                coroutineScope.launch {
                    inspecting.value = true
                    inspectionError.value = false
                    inspection.value = null
                    val url = url.value.trim()
                    require(url.isNotBlank())
                    try {
                        inspection.value = server.inspect(url)
                    } catch (e: Exception) {
                        inspectionError.value = true
                        e.printStackTrace()
                    }
                    inspecting.value = false
                }
            },
            onItemClick = { selected ->
                selectedItems.update { selectedItems ->
                    val exists = selectedItems.any { it == selected }
                    selectedItems.toMutableList().apply {
                        if (exists) { remove(selected) } else { add(selected) }
                    }.toList()
                }
            },
            onSelectAllClick = { selectedItems.value = inspection.value!!.items.map { it.id } },
            onUnselectAllClick = { selectedItems.value = emptyList() },
            onImportClick = {
                coroutineScope.launch {
                    savingImport.value = true
                    val selected = selectedItems.value
                    val url = url.value
                    val inspection = inspection.value
                    require(url.isNotBlank() && selected.isNotEmpty() && inspection != null && !inspectionError.value)
                    val sessionId = importSessionRepo.add(
                        uri = url,
                        sourceType = ImportSession.SourceType.Internet,
                        inspection = inspection.container.copy(thumbnail = null),
                        destinationFolderId = destinationFolderId,
                        imgId = inspection.container.thumbnail?.let { thumbnail ->
                            mediaFileRepo.add(null, Base64.decode(thumbnail))
                        }
                    )
                    for (item in inspection.items) {
                        importSessionItemRepo.add(
                            uri = item.uri,
                            importSessionId = sessionId,
                            state = if (selected.contains(item.id)) ImportSessionItem.State.Pending else ImportSessionItem.State.Nonselected,
                            title = item.title,
                            durationMilliseconds = item.durationMilliseconds,
                            album = item.album,
                            inspection = item.copy(thumbnail = null),
                            localFilePath = null,
                            albumTrackNumber = null,
                            lyrics = null,
                            year = null,
                            imgId = item.thumbnail?.let { thumbnail ->
                                mediaFileRepo.add(null, Base64.decode(thumbnail))
                            }
                        )
                    }
                    fileManager.saveYtDlpInspection(sessionId, inspection.ytDlpInspection)
                    showImportSession(sessionId, ImportSessionItem.State.Pending)
                    savingImport.value = false
                }
            }
        )
    }

    @Composable
    override fun show(modifier: Modifier) {
        Ui.Main(modifier, state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private object Ui {
        data class State(
            val inspecting: StateFlow<Boolean>,
            val savingImport: StateFlow<Boolean>,
            val inspectionError: StateFlow<Boolean>,
            val url: StateFlow<String>,
            val inspection: StateFlow<Inspection.Webpage?>,
            val selectedItems: StateFlow<List<Long>>,
            val onUrlChange: (String) -> Unit,
            val onInspectClick: () -> Unit,
            val onItemClick: (Long) -> Unit,
            val onSelectAllClick: () -> Unit,
            val onUnselectAllClick: () -> Unit,
            val onImportClick: () -> Unit
        )

        @Composable
        fun Main(modifier: Modifier, state: State) {
            Main(
                modifier = modifier,
                inspecting = state.inspecting,
                savingImport = state.savingImport,
                inspectionError = state.inspectionError,
                url = state.url,
                inspection = state.inspection,
                selectedItems = state.selectedItems,
                onUrlChange = state.onUrlChange,
                onInspectClick = state.onInspectClick,
                onItemClick = state.onItemClick,
                onSelectAllClick = state.onSelectAllClick,
                onUnselectAllClick = state.onUnselectAllClick,
                onImportClick = state.onImportClick,
            )
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            inspecting: StateFlow<Boolean>,
            savingImport: StateFlow<Boolean>,
            inspectionError: StateFlow<Boolean>,
            url: StateFlow<String>,
            inspection: StateFlow<Inspection.Webpage?>,
            selectedItems: StateFlow<List<Long>>,
            onUrlChange: (String) -> Unit,
            onInspectClick: () -> Unit,
            onItemClick: (Long) -> Unit,
            onSelectAllClick: () -> Unit,
            onUnselectAllClick: () -> Unit,
            onImportClick: () -> Unit
        ) {
            val inspecting by inspecting.collectAsState()
            val savingImport by savingImport.collectAsState()
            val inspectionError by inspectionError.collectAsState()
            val url by url.collectAsState()
            val inspection by inspection.collectAsState()
            val selectedItems by selectedItems.collectAsState()

            Surface(
                modifier = modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        label = { Text("Url") },
                        value = url,
                        onValueChange = onUrlChange,
                        enabled = !inspecting && !savingImport,
                        isError = inspectionError
                    )
                    Button(
                        onClick = onInspectClick,
                        enabled = url.isNotBlank() && !inspecting && !savingImport
                    ) {
                        Row {
                            Icon(Icons.Default.Search, null)
                            Text("Inspect")
                        }
                    }
                    inspection?.let { inspection ->
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onImportClick,
                            content = { Text("Import") },
                            enabled = !inspecting && !savingImport && !inspectionError && selectedItems.isNotEmpty() && url.isNotBlank()
                        )
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (selectedItems.size == inspection.items.size) {
                                    onUnselectAllClick()
                                } else {
                                    onSelectAllClick()
                                }
                            },
                            enabled = !inspecting && !savingImport
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedItems.size == inspection.items.size,
                                    onCheckedChange = null
                                )
                                Text("Select all")
                            }
                        }
                        ContainerInfo(
                            modifier = Modifier.fillMaxWidth(),
                            container = inspection.container
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items = inspection.items) { item ->
                                Track(
                                    modifier = Modifier.fillMaxWidth(),
                                    itemInspection = item,
                                    selected = selectedItems.any { it == item.id },
                                    onClick = { onItemClick(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun ContainerInfo(
            modifier: Modifier,
            container: Inspection.ContainerInspection.Webpage
        ) {
            Row(
                modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier.size(64.dp),
                    data = container.thumbnail?.let { Base64.decode(it) }
                )
                Text(
                    text = container.title.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        @Composable
        private fun Track(
            modifier: Modifier,
            itemInspection: Inspection.ItemInspection.InternetTrack,
            selected: Boolean,
            onClick: () -> Unit
        ) {
            Item(
                modifier = modifier,
                onClick = onClick,
                contentPadding = PaddingValues(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = null
                    )
                    Image(
                        modifier = Modifier.size(64.dp),
                        data = itemInspection.thumbnail?.let { Base64.decode(it) }

                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(itemInspection.title)
                        Text(
                            text = "" + itemInspection.album,
                            style = MaterialTheme.typography.labelMedium
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(itemInspection.artists) { artist ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, null)
                                    Text(
                                        text = artist,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Text(
                            text = itemInspection.durationMilliseconds.milliseconds.formatted(),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}