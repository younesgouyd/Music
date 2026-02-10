package dev.younesgouyd.apps.music.client.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.components.util.SystemFolderPicker
import dev.younesgouyd.apps.music.client.components.util.Uri
import dev.younesgouyd.apps.music.client.data.FolderId
import dev.younesgouyd.apps.music.client.data.ImportSessionId
import dev.younesgouyd.apps.music.client.data.repoes.FolderRepo
import dev.younesgouyd.apps.music.client.data.repoes.ImportSessionItemRepo
import dev.younesgouyd.apps.music.client.data.repoes.ImportSessionRepo
import dev.younesgouyd.apps.music.client.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSession
import dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.client.scanFolder
import dev.younesgouyd.apps.music.client.util.Component
import dev.younesgouyd.apps.music.common.Inspection
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64

class ImportFolderFlow(
    destinationFolderId: FolderId,
    folderRepo: FolderRepo,
    importSessionRepo: ImportSessionRepo,
    importSessionItemRepo: ImportSessionItemRepo,
    mediaFileRepo: MediaFileRepo,
    showImportSession: (ImportSessionId, ImportSessionItem.State) -> Unit
) : Component() {
    override val title: String = "Import Folder"
    private var job: Job? = null
    private val state: MutableStateFlow<Ui.State> = MutableStateFlow(Ui.State.Loading)

    init {
        val isPreparing = MutableStateFlow(false)
        val showCancelButton = MutableStateFlow(false)
        coroutineScope.launch {
            state.value = Ui.State.Loaded(
                destination = run {
                    var result = ""
                    var id: FolderId? = destinationFolderId
                    while (id != null) {
                        val folder = folderRepo.get(id).first()
                        result = "${folder.name}/$result"
                        id = folder.parentFolderId
                    }
                    result = "root/$result"
                    result
                },
                isPreparing = isPreparing.asStateFlow(),
                showCancelButton = showCancelButton.asStateFlow(),
                onFolderSelected = { uri: String ->
                    job = coroutineScope.launch {
                        isPreparing.value = true
                        showCancelButton.value = true
                        val items = scanFolder(uri)
                        showCancelButton.value = false
                        val inspection = Inspection.Folder(
                            container = Inspection.ContainerInspection.Folder(uri = uri),
                            items = items
                        )
                        require(inspection.container.uri.isNotBlank() && inspection.items.isNotEmpty())
                        val sessionId = importSessionRepo.add(
                            uri = inspection.container.uri,
                            sourceType = ImportSession.SourceType.Local,
                            inspection = inspection.container,
                            destinationFolderId = destinationFolderId,
                            imgId = null
                        )
                        for (item in inspection.items) {
                            importSessionItemRepo.add(
                                uri = item.uri,
                                importSessionId = sessionId,
                                state = ImportSessionItem.State.Pending,
                                title = item.title,
                                durationMilliseconds = item.durationMilliseconds,
                                artists = item.artists.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"$it\"" },
                                album = item.album,
                                inspection = item.copy(albumImage = null),
                                localFilePath = item.path.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"$it\"" },
                                albumTrackNumber = item.albumTrackNumber,
                                lyrics = item.lyrics,
                                year = item.year,
                                imgId = item.albumImage?.let { albumImage ->
                                    mediaFileRepo.add(null, Base64.decode(albumImage))
                                }
                            )
                        }
                        showImportSession(sessionId, ImportSessionItem.State.Pending)
                        isPreparing.value = false
                    }
                },
                onCancelClick = {
                    job!!.cancel()
                    isPreparing.value = false
                }
            )
        }
    }

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(modifier, state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private object Ui {
        sealed class State {
            data object Loading : State()

            data class Loaded(
                val destination: String, // TODO: handle the case where the destination folder no longer exists mid-operation
                val isPreparing: StateFlow<Boolean>,
                val showCancelButton: StateFlow<Boolean>,
                val onFolderSelected: (Uri) -> Unit,
                val onCancelClick: () -> Unit
            ) : State()
        }

        @Composable
        fun Main(modifier: Modifier, state: State) {
            when (state) {
                is State.Loading -> {
                    Surface(
                        modifier = modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Loading...")
                        }
                    }
                }
                is State.Loaded -> Main(modifier, state)
            }
        }

        @Composable
        private fun Main(modifier: Modifier, state: State.Loaded) {
            Main(
                modifier = modifier,
                destination = state.destination,
                isPreparing = state.isPreparing,
                showCancelButton = state.showCancelButton,
                onFolderSelected = state.onFolderSelected,
                onCancelClick = state.onCancelClick
            )
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            destination: String,
            isPreparing: StateFlow<Boolean>,
            showCancelButton: StateFlow<Boolean>,
            onFolderSelected: (Uri) -> Unit,
            onCancelClick: () -> Unit
        ) {
            val isPreparing by isPreparing.collectAsState()
            val showCancelButton by showCancelButton.collectAsState()
            var showSystemFolderPicker: Boolean by remember { mutableStateOf(false) }

            Surface(
                modifier = modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Destination: $destination")
                    when (isPreparing) {
                        false -> {
                            Button(
                                onClick = { showSystemFolderPicker = true },
                                content = { Text("Open Folder Picker") }
                            )
                        }
                        true -> {
                            Text("Please wait, preparing import...")
                            if (showCancelButton) {
                                Button(
                                    onClick = onCancelClick,
                                    content = { Text("Cancel") }
                                )
                            }
                        }
                    }
                }
            }

            if (showSystemFolderPicker) {
                SystemFolderPicker(
                    onFolderChosen = {
                        showSystemFolderPicker = false
                        onFolderSelected(it)
                    },
                    onCancelled = { showSystemFolderPicker = false }
                )
            }
        }
    }
}