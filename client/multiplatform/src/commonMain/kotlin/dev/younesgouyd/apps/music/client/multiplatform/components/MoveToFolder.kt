package dev.younesgouyd.apps.music.client.multiplatform.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    tracks: Set<TrackId>,
    playlists: Set<PlaylistId>,
    folders: Set<FolderId>,
    folderRepo: FolderRepo,
    moveItemsToFolderUseCase: MoveItemsToFolderUseCase,
    dismiss: () -> Unit
) : Component() {
    override val title: String = "Move to Folder"
    private val _moving: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val state: Ui.State

    val moving get() = _moving.asStateFlow()

    init {
        val path = MutableStateFlow(listOf<Folder?>(null))
        val currentFolder: StateFlow<Folder?> =  path.map { it.last() }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
        state = Ui.State.Loaded(
            loading = _moving.asStateFlow(),
            path = path,
            folders = currentFolder.flatMapLatest { folderRepo.getSubfolders(it?.id) }
                .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList()),
            openFolder = { folder ->
                path.update { list ->
                    if (folder == null) return@update list.take(1)
                    val i = list.indexOfFirst { it?.id == folder.id }
                    if (i >= 0) list.take(i + 1) else list + folder
                }
            },
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

    @Composable
    override fun show(modifier: Modifier) {
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
                val loading: StateFlow<Boolean>,
                val path: StateFlow<List<Folder?>>,
                val folders: StateFlow<List<Folder>>,
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
            val loading by loaded.loading.collectAsState()
            val path by loaded.path.collectAsState()
            val folders by loaded.folders.collectAsState()
            val lazyColumnState = rememberLazyListState()

            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.background
            ) {
                if (loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
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
                                path = path.filterNotNull(),
                                onFolderClick = loaded.openFolder
                            )
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
                        Button(
                            content = { Text("Move here") },
                            onClick = loaded.move
                        )
                    }
                }
            }
        }

        @Composable
        private fun Path(
            modifier: Modifier,
            path: List<Folder>,
            onFolderClick: (Folder?) -> Unit
        ) {
            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                content = { Text(".") },
                                onClick = { onFolderClick(null) }
                            )
                            Text("/")
                        }
                    }
                    items(items = path) { folder ->
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                content = { Text(folder.name) },
                                onClick = { onFolderClick(folder) }
                            )
                            Text("/")
                        }
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