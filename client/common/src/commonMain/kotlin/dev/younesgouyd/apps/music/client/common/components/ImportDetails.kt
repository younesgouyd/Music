package dev.younesgouyd.apps.music.client.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.common.components.util.formatted
import dev.younesgouyd.apps.music.client.common.data.repoes.ImportSessionItemRepo
import dev.younesgouyd.apps.music.client.common.data.repoes.ImportSessionRepo
import dev.younesgouyd.apps.music.client.common.data.repoes.MediaFileRepo
import dev.younesgouyd.apps.music.client.common.usecases.ClearImportItemUseCase
import dev.younesgouyd.apps.music.client.common.util.Component
import dev.younesgouyd.apps.music.common.models.DbOrder
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.ImportSessionItem
import dev.younesgouyd.apps.music.common.models.ImportSessionItemId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ImportDetails(
    id: ImportSessionId,
    defaultTab: ImportSessionItem.State,
    importSessionRepo: ImportSessionRepo,
    importSessionItemRepo: ImportSessionItemRepo,
    mediaFileRepo: MediaFileRepo,
    clearImportItemUseCase: ClearImportItemUseCase,
    showImportItem: (ImportSessionItemId) -> Unit
) : Component() {
    override val title: String = "Import"
    private val state: StateFlow<Ui.State>

    init {
        val searchQuery = MutableStateFlow("")
        val import = importSessionRepo.get(id).filterNotNull().mapLatest { dbImport ->
            Ui.State.Loaded.Import(
                id = dbImport.id,
                uri = dbImport.uri,
                title = dbImport.inspection.title,
                description = dbImport.inspection.description,
                image = mediaFileRepo.getImportSessionImage(dbImport.id),
                createdAt = Instant.fromEpochMilliseconds(dbImport.creationDatetime).toString()
            )
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
        var loaded: Ui.State.Loaded? = null
        state = import.onEach {
            if (it != null && loaded == null) {
                loaded = Ui.State.Loaded(
                    defaultTab = defaultTab,
                    import = import.filterNotNull().stateIn(coroutineScope),
                    items = Ui.State.Loaded.Items(
                        nonselected = searchQuery.flatMapLatest {
                            importSessionItemRepo.search(id, ImportSessionItem.State.Nonselected, it, DbOrder.Ascending)
                                .mapLatest { list -> list.map { it.toModel(mediaFileRepo) } }
                        }.stateIn(coroutineScope),
                        pending = searchQuery.flatMapLatest {
                            importSessionItemRepo.search(id, ImportSessionItem.State.Pending, it, DbOrder.Ascending)
                                .mapLatest { list -> list.map { it.toModel(mediaFileRepo) } }
                        }.stateIn(coroutineScope),
                        inProgress = searchQuery.flatMapLatest {
                            importSessionItemRepo.search(id, ImportSessionItem.State.InProgress, it, DbOrder.Ascending)
                                .mapLatest { list -> list.map { it.toModel(mediaFileRepo) } }
                        }.stateIn(coroutineScope),
                        completed = searchQuery.flatMapLatest {
                            importSessionItemRepo.search(id, ImportSessionItem.State.Completed, it, DbOrder.Descending)
                                .mapLatest { list -> list.map { it.toModel(mediaFileRepo) } }
                        }.stateIn(coroutineScope),
                        cancelled = searchQuery.flatMapLatest {
                            importSessionItemRepo.search(id, ImportSessionItem.State.Cancelled, it, DbOrder.Ascending)
                                .mapLatest { list -> list.map { it.toModel(mediaFileRepo) } }
                        }.stateIn(coroutineScope),
                        failed = searchQuery.flatMapLatest {
                            importSessionItemRepo.search(id, ImportSessionItem.State.Failed, it, DbOrder.Ascending)
                                .mapLatest { list -> list.map { it.toModel(mediaFileRepo) } }
                        }.stateIn(coroutineScope),
                    ),
                    searchQuery = searchQuery.asStateFlow(),
                    onSearchQueryChange = { searchQuery.value = it },
                    onItemClick = showImportItem,
                    onImportItemClick = {
                        coroutineScope.launch {
                            importSessionItemRepo.updateState(id = it, state = ImportSessionItem.State.Pending)
                        }
                    },
                    onCancelItemClick = { TODO() },
                    onClearItemClick = { id ->
                        coroutineScope.launch {
                            clearImportItemUseCase.execute(id)
                        }
                    },
                    onRetryItemClick = { TODO() }
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

        Ui.Main(modifier, state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private suspend fun ImportSessionItem.toModel(mediaFileRepo: MediaFileRepo): Ui.State.Loaded.Items.Item {
        return Ui.State.Loaded.Items.Item(
            id = this.id,
            uri = this.uri,
            state = this.state,
            title = this.inspection.title,
            duration = this.inspection.durationMilliseconds.milliseconds,
            artists = this.inspection.artists,
            album = this.inspection.album,
            image = mediaFileRepo.getImportSessionItemImage(this.id)
        )
    }


    private object Ui {
        sealed class State {
            data object Loading : State()

            data class Loaded(
                val defaultTab: ImportSessionItem.State,
                val import: StateFlow<Import>,
                val items: Items,
                val searchQuery: StateFlow<String>,
                val onSearchQueryChange: (String) -> Unit,
                val onItemClick: (ImportSessionItemId) -> Unit,
                val onImportItemClick: (ImportSessionItemId) -> Unit,
                val onCancelItemClick: (ImportSessionItemId) -> Unit,
                val onClearItemClick: (ImportSessionItemId) -> Unit,
                val onRetryItemClick: (ImportSessionItemId) -> Unit
            ) : State() {
                data class Import(
                    val id: ImportSessionId,
                    val uri: String,
                    val title: String?,
                    val description: String?,
                    val image: File?,
                    val createdAt: String
                )

                data class Items(
                    val nonselected: StateFlow<List<Item>>,
                    val pending: StateFlow<List<Item>>,
                    val inProgress: StateFlow<List<Item>>,
                    val completed: StateFlow<List<Item>>,
                    val cancelled: StateFlow<List<Item>>,
                    val failed: StateFlow<List<Item>>
                ) {
                    data class Item(
                        val id: ImportSessionItemId,
                        val uri: String,
                        val state: ImportSessionItem.State,
                        val title: String,
                        val duration: Duration,
                        val artists: List<String>,
                        val album: String?,
                        val image: File?
                    )
                }
            }

            data object ItemDoesNotExist : State()
        }

        @Composable
        fun Main(modifier: Modifier, state: State) {
            when (state) {
                is State.Loading -> Text(modifier = modifier, text = "Loading...")
                is State.Loaded -> Main(modifier = modifier, loaded = state)
                is State.ItemDoesNotExist -> Text(modifier = modifier, text = "This item no long exists")
            }
        }

        @Composable
        private fun Main(modifier: Modifier, loaded: State.Loaded) {
            Main(
                modifier = modifier,
                defaultTab = loaded.defaultTab,
                import = loaded.import,
                items = loaded.items,
                searchQuery = loaded.searchQuery,
                onSearchQueryChange = loaded.onSearchQueryChange,
                onItemClick = loaded.onItemClick,
                onImportItemClick = loaded.onImportItemClick,
                onCancelItemClick = loaded.onCancelItemClick,
                onClearItemClick = loaded.onClearItemClick,
                onRetryItemClick = loaded.onRetryItemClick
            )
        }

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        private fun Main(
            modifier: Modifier,
            defaultTab: ImportSessionItem.State,
            import: StateFlow<State.Loaded.Import>,
            items: State.Loaded.Items,
            searchQuery: StateFlow<String>,
            onSearchQueryChange: (String) -> Unit,
            onItemClick: (ImportSessionItemId) -> Unit,
            onImportItemClick: (ImportSessionItemId) -> Unit,
            onCancelItemClick: (ImportSessionItemId) -> Unit,
            onClearItemClick: (ImportSessionItemId) -> Unit,
            onRetryItemClick: (ImportSessionItemId) -> Unit
        ) {
            val import by import.collectAsState()
            val nonselected by items.nonselected.collectAsState()
            val pending by items.pending.collectAsState()
            val inProgress by items.inProgress.collectAsState()
            val completed by items.completed.collectAsState()
            val cancelled by items.cancelled.collectAsState()
            val failed by items.failed.collectAsState()
            val searchQuery by searchQuery.collectAsState()
            val items: Map<ImportSessionItem.State, List<State.Loaded.Items.Item>> = mapOf(
                ImportSessionItem.State.Nonselected to nonselected,
                ImportSessionItem.State.Pending to pending,
                ImportSessionItem.State.InProgress to inProgress,
                ImportSessionItem.State.Completed to completed,
                ImportSessionItem.State.Cancelled to cancelled,
                ImportSessionItem.State.Failed to failed
            )
            var selected: Pair<Int, ImportSessionItem.State> by remember { mutableStateOf(items.keys.indexOf(defaultTab) to defaultTab) }
            val currentItems = items[selected.second]!!

            Scaffold(
                modifier = modifier.fillMaxSize(),
                content = { paddingValues ->
                    Box(Modifier.fillMaxSize().padding(paddingValues)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            ImportInfo(
                                modifier = Modifier.fillMaxWidth(),
                                import = import
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SecondaryScrollableTabRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    selectedTabIndex = selected.first
                                ) {
                                    ImportSessionItem.State.entries.forEachIndexed { index, item ->
                                        Tab(
                                            text = { Text(item.name) },
                                            selected = false,
                                            onClick = { selected = index to item }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Search, null) },
                                    label = { Text("Search") },
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange
                                )
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    contentPadding = PaddingValues(8.dp)
                                ) {
                                    items(currentItems) { item ->
                                        ImportItem(
                                            modifier = Modifier.fillMaxWidth(),
                                            item = item,
                                            onClick = { onItemClick(item.id) },
                                            onImportClick = { onImportItemClick(item.id) },
                                            onCancelClick = { onCancelItemClick(item.id) },
                                            onClearClick = { onClearItemClick(item.id) },
                                            onRetryClick = { onRetryItemClick(item.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        @Composable
        private fun ImportInfo(
            modifier: Modifier,
            import: State.Loaded.Import
        ) {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dev.younesgouyd.apps.music.client.common.components.util.Image(
                    modifier = Modifier.size(64.dp),
                    file = import.image
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Session: ${import.id}")
                    Text(import.uri)
                    Text(import.createdAt)
                    Text(import.title ?: "")
                    Text(import.description ?: "")
                }
            }
        }

        @Composable
        private fun ImportItem(
            modifier: Modifier,
            item: State.Loaded.Items.Item,
            onClick: () -> Unit,
            onImportClick: () -> Unit,
            onCancelClick: () -> Unit,
            onClearClick: () -> Unit,
            onRetryClick: () -> Unit
        ) {
            dev.younesgouyd.apps.music.client.common.components.util.Item(
                modifier = modifier,
                onClick = onClick,
                contentPadding = PaddingValues(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dev.younesgouyd.apps.music.client.common.components.util.Image(
                        modifier = Modifier.size(64.dp).aspectRatio(1f),
                        file = item.image
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(item.title)
                        Text(
                            text = "" + item.album,
                            style = MaterialTheme.typography.labelMedium
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(item.artists) { artist ->
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
                            text = item.duration.formatted(),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    when (item.state) {
                        ImportSessionItem.State.Nonselected -> {
                            IconButton(
                                onClick = onImportClick,
                                content = { Icon(Icons.Default.ImportExport, null) }
                            )
                        }

                        ImportSessionItem.State.Pending,
                        ImportSessionItem.State.InProgress -> {
                            IconButton(
                                onClick = onCancelClick,
                                content = { Icon(Icons.Default.Cancel, null) }
                            )
                        }

                        ImportSessionItem.State.Completed -> {
                            IconButton(
                                onClick = onClearClick,
                                content = { Icon(Icons.Default.Remove, "Clear") }
                            )
                        }

                        ImportSessionItem.State.Cancelled,
                        ImportSessionItem.State.Failed -> {
                            IconButton(
                                onClick = onRetryClick,
                                content = { Icon(Icons.Default.RestartAlt, null) }
                            )
                        }
                    }
                }
            }
        }
    }
}