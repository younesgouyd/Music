package dev.younesgouyd.apps.music.client.multiplatform.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.younesgouyd.apps.music.client.multiplatform.components.util.Path
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.FolderRepo
import dev.younesgouyd.apps.music.client.multiplatform.usecases.MoveItemsToFolderUseCase
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.PlaylistId
import dev.younesgouyd.apps.music.common.models.TrackId
import dev.younesgouyd.apps.music.common.models.datamodels.Folder
import dev.younesgouyd.apps.music.common.ui.Component
import dev.younesgouyd.apps.music.common.ui.Item
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MoveToFolder(
    startFolder: FolderId?,
    tracks: Set<TrackId>,
    playlists: Set<PlaylistId>,
    folders: Set<FolderId>,
    folderRepo: FolderRepo,
    moveItemsToFolderUseCase: MoveItemsToFolderUseCase,
    dismiss: () -> Unit
) : Component() {
    override val title: String = "Move to Folder"
    private val _moving: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val state: MutableStateFlow<Ui.State> = MutableStateFlow(Ui.State.Loading)
    private val searchQuery = MutableStateFlow("")
    private val path: MutableStateFlow<List<Folder?>> = MutableStateFlow(listOf(null))
    val currentFolder: StateFlow<Folder?> = path.map { it.last() }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    private val loadingFolders = MutableStateFlow(true)

    val moving get() = _moving.asStateFlow()

    init {
        coroutineScope.launch {
            path.value = run {
                if (startFolder == null) {
                    listOf(null)
                } else {
                    val mut = mutableListOf<Folder?>()
                    var parent = startFolder
                    while (parent != null) {
                        val f = folderRepo.get(parent).first()
                        if (f == null) {
                            mut.clear()
                            break
                        }
                        mut.add(f)
                        parent = f.parentFolderId
                    }
                    mut.add(null)
                    mut.reverse()
                    mut.toList().takeWhile { it?.id !in folders }
                }
            }

            state.value = Ui.State.Loaded(
                moving = moving,
                path = path,
                searchQuery = searchQuery.asStateFlow(),
                folders = combine(currentFolder, searchQuery, ::Pair)
                    .onEach { loadingFolders.value = true }
                    .flatMapLatest { (folder, search) -> folderRepo.searchFolder(folder?.id, search) }
                    .map { it.filterNot { it.id in folders } }
                    .onEach { loadingFolders.value = false }
                    .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList()),
                loadingFolders = loadingFolders.asStateFlow(),
                onSearchQueryChange = { searchQuery.value = it },
                onNewFolder = {
                    coroutineScope.launch {
                        val id = folderRepo.add(name = it, parentFolderId = currentFolder.value?.id)
                        val folder = folderRepo.get(id).first()
                        openFolder(folder)
                    }
                },
                openFolder = ::openFolder,
                move = {
                    coroutineScope.launch {
                        _moving.update { true }
                        moveItemsToFolderUseCase.execute(tracks, playlists, folders, currentFolder.value?.id)
                        _moving.update { false }
                        dismiss()
                    }
                }
            )
        }
    }

    private fun openFolder(folder: Folder?) {
        path.update { list ->
            if (folder == null) return@update list.take(1)
            val i = list.indexOfFirst { it?.id == folder.id }
            if (i >= 0) list.take(i + 1) else list + folder
        }
    }

    @Composable
    override fun show(modifier: Modifier) {
        val state by state.collectAsState()

        Ui.Main(
            modifier = modifier.size(width = 500.dp, height = 600.dp),
            state = state
        )
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private object Ui {
        sealed class State {
            data object Loading : State()

            class Loaded(
                val moving: StateFlow<Boolean>,
                val path: StateFlow<List<Folder?>>,
                val searchQuery: StateFlow<String>,
                val folders: StateFlow<List<Folder>>,
                val loadingFolders: StateFlow<Boolean>,
                val onSearchQueryChange: (String) -> Unit,
                val onNewFolder: (name: String) -> Unit,
                val openFolder: (Folder?) -> Unit,
                val move: () -> Unit
            ) : State()
        }

        @Composable
        fun Main(
            modifier: Modifier,
            state: State
        ) {
            when (state) {
                is State.Loading -> Text(modifier = modifier, text = "Loading...")
                is State.Loaded -> Main(modifier = modifier, loaded = state)
            }
        }

        @Composable
        fun Main(
            modifier: Modifier,
            loaded: State.Loaded
        ) {
            val loading by loaded.moving.collectAsState()
            val path by loaded.path.collectAsState()
            val folders by loaded.folders.collectAsState()
            val loadingFolders by loaded.loadingFolders.collectAsState()
            val lazyColumnState = rememberLazyListState()

            var newFolderFormVisible by remember { mutableStateOf(false) }

            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.background
            ) {
                if (loading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                    ) {
                        CircularProgressIndicator()
                        Text(text = "Please wait...")
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                text = "Move to Folder",
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center
                            )
                            Path(
                                modifier = Modifier.fillMaxWidth(),
                                path = path,
                                searchQuery = loaded.searchQuery,
                                onFolderClick = loaded.openFolder,
                                onSearchQueryChange = loaded.onSearchQueryChange
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { newFolderFormVisible = true },
                                    content = { Icon(Icons.Default.CreateNewFolder, null) }
                                )
                                VerticalDivider()
                                IconButton(
                                    onClick = { /* todo - sort by alpha */ },
                                    content = { Icon(Icons.Default.SortByAlpha, null) }
                                )
                                IconButton(
                                    onClick = { /* todo - sort by date */ },
                                    content = { Icon(Icons.AutoMirrored.Default.Sort, null) }
                                )
                            }
                            if (loadingFolders) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                                    state = lazyColumnState,
                                    contentPadding = PaddingValues(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(
                                        items = folders,
                                        key = { it.id.value }
                                    ) { folder ->
                                        FolderItem(
                                            folder = folder,
                                            onClick = { loaded.openFolder(folder) }
                                        )
                                    }
                                }
                            }
                        }
                        Button(
                            content = { Text("Move here") },
                            onClick = loaded.move
                        )
                    }
                }
            }

            if (newFolderFormVisible) {
                NewFolderDialog(
                    title = "New folder",
                    onDone = { loaded.onNewFolder(it); newFolderFormVisible = false },
                    onDismiss = { newFolderFormVisible = false }
                )
            }
        }

        @Composable
        private fun NewFolderDialog(
            title: String,
            name: String = "",
            onDone: (name: String) -> Unit,
            onDismiss: () -> Unit
        ) {
            var name by remember { mutableStateOf(name) }

            Dialog(onDismissRequest = onDismiss) {
                Surface(
                    modifier = Modifier.width(500.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Name") },
                            value = name,
                            onValueChange = { name = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onDone(name) }),
                        )
                        Button(
                            content = { Text("Done") },
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onDone(name) }
                        )
                    }
                }
            }
        }

        @Composable
        private fun FolderItem(
            folder: Folder,
            onClick: () -> Unit
        ) {
            Item(
                onClick = onClick,
                contentPadding = PaddingValues(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}