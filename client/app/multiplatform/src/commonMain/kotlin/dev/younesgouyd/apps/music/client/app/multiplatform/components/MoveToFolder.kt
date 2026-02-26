package dev.younesgouyd.apps.music.client.app.multiplatform.components

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
import dev.younesgouyd.apps.music.client.app.multiplatform.components.util.Image
import dev.younesgouyd.apps.music.client.app.multiplatform.components.util.Item
import dev.younesgouyd.apps.music.client.app.multiplatform.data.FolderId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.PlaylistId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.TrackId
import dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes.FolderRepo
import dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes.PlaylistRepo
import dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes.TrackRepo
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.Folder
import dev.younesgouyd.apps.music.client.app.multiplatform.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class MoveToFolder(
    itemToMove: ItemToMove,
    folderRepo: FolderRepo,
    trackRepo: TrackRepo,
    mediaFileRepo: MediaFileRepo,
    playlistRepo: PlaylistRepo,
    dismiss: () -> Unit
) : Component() {
    override val title: String = "Move to Folder"
    private val _moving: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val state: StateFlow<Ui.State>

    val moving get() = _moving.asStateFlow()

    init {
        val currentFolder: MutableStateFlow<Folder?> = MutableStateFlow(null)
        var list: List<Folder?> = listOf(null)
        val item = when (itemToMove) {
            is ItemToMove.Track -> trackRepo.get(itemToMove.id).filterNotNull().map { dbTrack ->
                Ui.State.Loaded.ItemToAdd(
                    name = dbTrack.spotifyTrack?.name ?: dbTrack.originalImport.title,
                    image = if (dbTrack.spotifyTrack != null) {
                        mediaFileRepo.getSpotifyAlbumImage(dbTrack.spotifyTrack.spotifyAlbumId)
                    } else {
                        mediaFileRepo.getImportSessionItemImage(dbTrack.track.importSessionItemId)
                    }
                )
            }
            is ItemToMove.Playlist -> playlistRepo.get(itemToMove.id).filterNotNull().map { dbPlaylist ->
                Ui.State.Loaded.ItemToAdd(
                    name = dbPlaylist.name,
                    image = null
                )
            }
            is ItemToMove.Folder -> folderRepo.get(itemToMove.id).filterNotNull().map { dbFolder ->
                Ui.State.Loaded.ItemToAdd(
                    name = dbFolder.name,
                    image = null
                )
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
        var loaded: Ui.State.Loaded? = null
        state = item.onEach {
            if (it != null && loaded == null) {
                loaded = Ui.State.Loaded(
                    loading = this@MoveToFolder._moving.asStateFlow(),
                    itemToAdd = item.filterNotNull().stateIn(coroutineScope),
                    currentFolder = currentFolder.asStateFlow(),
                    path = flow {
                        fun <T> List<T>.takeUntil(predicate: (T) -> Boolean): List<T> {
                            val list = mutableListOf<T>()
                            for (item in this) {
                                list.add(item)
                                if (predicate(item)) {
                                    break
                                }
                            }
                            return list
                        }
                        currentFolder.collect { folder ->
                            if (folder == null) {
                                list = listOf(null)
                                emit(list)
                            } else {
                                val temp = list.takeUntil { it?.id == folder.id }.toMutableList()
                                if (!temp.any { it?.id == folder.id }) {
                                    temp.add(folder)
                                }
                                list = temp.toList()
                                emit(list)
                            }
                        }
                    }.stateIn(coroutineScope),
                    folders = currentFolder.flatMapLatest { folderRepo.getSubfolders(it?.id) }.stateIn(coroutineScope),
                    openFolder = { currentFolder.value = it },
                    move = {
                        coroutineScope.launch {
                            this@MoveToFolder._moving.update { true }
                            when (itemToMove) {
                                is ItemToMove.Track -> trackRepo.updateFolderId(itemToMove.id, currentFolder.value!!.id)
                                is ItemToMove.Playlist -> playlistRepo.updateFolderId(itemToMove.id, currentFolder.value!!.id)
                                is ItemToMove.Folder -> folderRepo.updateParentFolderId(itemToMove.id, currentFolder.value!!.id)
                            }
                            this@MoveToFolder._moving.update { false }
                            dismiss()
                        }
                    }
                )
            }
        }.map {
            if (it == null) {
                Ui.State.ItemDoesNotExist
            } else {
                loaded!!
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), Ui.State.Loading)
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

    sealed class ItemToMove {
        data class Track(val id: TrackId) : ItemToMove()

        data class Playlist(val id: PlaylistId) : ItemToMove()

        data class Folder(val id: FolderId) : ItemToMove()
    }

    private object Ui {
        sealed class State {
            data object Loading : State()

            data class Loaded(
                val loading: StateFlow<Boolean>,
                val itemToAdd: StateFlow<ItemToAdd>,
                val currentFolder: StateFlow<Folder?>,
                val path: StateFlow<List<Folder?>>,
                val folders: StateFlow<List<Folder>>,
                val openFolder: (Folder?) -> Unit,
                val move: () -> Unit
            ) : State() {
                data class ItemToAdd(
                    val name: String,
                    val image: File?
                )
            }

            data object ItemDoesNotExist : State()
        }

        @Composable
        fun Main(
            modifier: Modifier,
            state: State
        ) {
            when (state) {
                is State.Loading -> Text(modifier = modifier, text = "Loading...")
                is State.Loaded -> Main(modifier = modifier, loaded = state)
                is State.ItemDoesNotExist -> Text(modifier = modifier, text = "This item no long exists")
            }
        }

        @Composable
        fun Main(
            modifier: Modifier,
            loaded: State.Loaded
        ) {
            val loading by loaded.loading.collectAsState()
            val itemToAdd by loaded.itemToAdd.collectAsState()
            val currentFolder by loaded.currentFolder.collectAsState()
            val path by loaded.path.collectAsState()
            val folders by loaded.folders.collectAsState()
            val lazyColumnState = rememberLazyListState()

            if (!loading) {
                Surface(
                    modifier = modifier,
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.background
                ) {
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
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    modifier = Modifier.size(64.dp),
                                    file = itemToAdd.image
                                )
                                Text(
                                    text = itemToAdd.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                            enabled = currentFolder != null,
                            onClick = loaded.move
                        )
                    }
                }
            } else {
                Text(modifier = modifier, text = "Please wait...")
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