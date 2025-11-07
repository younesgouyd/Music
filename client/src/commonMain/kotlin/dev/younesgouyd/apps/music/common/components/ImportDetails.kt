package dev.younesgouyd.apps.music.common.components

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
import dev.younesgouyd.apps.music.common.Inspection
import dev.younesgouyd.apps.music.common.components.util.compose.formatted
import dev.younesgouyd.apps.music.common.components.util.compose.widgets.Image
import dev.younesgouyd.apps.music.common.components.util.compose.widgets.Item
import dev.younesgouyd.apps.music.common.data.repoes.ImportSessionItemRepo
import dev.younesgouyd.apps.music.common.data.repoes.ImportSessionRepo
import dev.younesgouyd.apps.music.common.data.room.entities.ImportSession
import dev.younesgouyd.apps.music.common.data.room.entities.ImportSessionItem
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ImportDetails(
    id: Long,
    importSessionRepo: ImportSessionRepo,
    importSessionItemRepo: ImportSessionItemRepo
) : Component() {
    override val title: String = "Import"
    private val state: MutableStateFlow<ImportDetailsState> = MutableStateFlow(ImportDetailsState.Loading)
    private val searchQuery = MutableStateFlow("")

    init {
        coroutineScope.launch {
            state.value = ImportDetailsState.Loaded(
                import = importSessionRepo.get(id).stateIn(coroutineScope),
                items = ImportDetailsState.Loaded.Items(
                    nonselected = searchQuery.flatMapLatest { importSessionItemRepo.search(id, ImportSessionItem.State.Nonselected, it) }.stateIn(coroutineScope),
                    pending = searchQuery.flatMapLatest { importSessionItemRepo.search(id, ImportSessionItem.State.Pending, it) }.stateIn(coroutineScope),
                    inProgress = searchQuery.flatMapLatest { importSessionItemRepo.search(id, ImportSessionItem.State.InProgress, it) }.stateIn(coroutineScope),
                    completed = searchQuery.flatMapLatest { importSessionItemRepo.search(id, ImportSessionItem.State.Completed, it) }.stateIn(coroutineScope),
                    cancelled = searchQuery.flatMapLatest { importSessionItemRepo.search(id, ImportSessionItem.State.Cancelled, it) }.stateIn(coroutineScope),
                    failed = searchQuery.flatMapLatest { importSessionItemRepo.search(id, ImportSessionItem.State.Failed, it) }.stateIn(coroutineScope)
                ),
                searchQuery = searchQuery.asStateFlow(),
                onSearchQueryChange = { searchQuery.value = it },
                onImportItemClick = {
                    coroutineScope.launch {
                        importSessionItemRepo.updateState(id = it, state = ImportSessionItem.State.Pending)
                    }
                },
                onCancelItemClick = {
                    coroutineScope.launch {
                        importSessionItemRepo.updateState(id = it, state = ImportSessionItem.State.Cancelled)
                    }
                },
                onRetryItemClick = {
                    coroutineScope.launch {
                        importSessionItemRepo.updateState(id = it, state = ImportSessionItem.State.Pending)
                    }
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

    private sealed class ImportDetailsState {
        data object Loading : ImportDetailsState()

        data class Loaded(
            val import: StateFlow<ImportSession>,
            val items: Items,
            val searchQuery: StateFlow<String>,
            val onSearchQueryChange: (String) -> Unit,
            val onImportItemClick: (Long) -> Unit,
            val onCancelItemClick: (Long) -> Unit,
            val onRetryItemClick: (Long) -> Unit
        ) : ImportDetailsState() {
            data class Items(
                val nonselected: StateFlow<List<ImportSessionItem>>,
                val pending: StateFlow<List<ImportSessionItem>>,
                val inProgress: StateFlow<List<ImportSessionItem>>,
                val completed: StateFlow<List<ImportSessionItem>>,
                val cancelled: StateFlow<List<ImportSessionItem>>,
                val failed: StateFlow<List<ImportSessionItem>>
            )
        }
    }

    private object Ui {
        @Composable
        fun Main(modifier: Modifier, state: ImportDetailsState) {
            when (state) {
                is ImportDetailsState.Loading -> Text(modifier = modifier, text = "Loading...")
                is ImportDetailsState.Loaded -> Main(modifier = modifier, state = state)
            }
        }

        @Composable
        private fun Main(modifier: Modifier, state: ImportDetailsState.Loaded) {
            Main(
                modifier = modifier,
                import = state.import,
                items = state.items,
                searchQuery = state.searchQuery,
                onSearchQueryChange = state.onSearchQueryChange,
                onImportItemClick = state.onImportItemClick,
                onCancelItemClick = state.onCancelItemClick,
                onRetryItemClick = state.onRetryItemClick
            )
        }
        
        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        private fun Main(
            modifier: Modifier,
            import: StateFlow<ImportSession>,
            items: ImportDetailsState.Loaded.Items,
            searchQuery: StateFlow<String>,
            onSearchQueryChange: (String) -> Unit,
            onImportItemClick: (Long) -> Unit,
            onCancelItemClick: (Long) -> Unit,
            onRetryItemClick: (Long) -> Unit
        ) {
            val import by import.collectAsState()
            val nonselected by items.nonselected.collectAsState()
            val pending by items.pending.collectAsState()
            val inProgress by items.inProgress.collectAsState()
            val completed by items.completed.collectAsState()
            val cancelled by items.cancelled.collectAsState()
            val failed by items.failed.collectAsState()
            val searchQuery by searchQuery.collectAsState()
            val items: Map<ImportSessionItem.State, List<ImportSessionItem>> = mapOf(
                ImportSessionItem.State.Nonselected to nonselected,
                ImportSessionItem.State.Pending to pending,
                ImportSessionItem.State.InProgress to inProgress,
                ImportSessionItem.State.Completed to completed,
                ImportSessionItem.State.Cancelled to cancelled,
                ImportSessionItem.State.Failed to failed
            )
            var selected: Pair<Int, ImportSessionItem.State> by remember { mutableStateOf(0 to ImportSessionItem.State.Nonselected) }
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
                                            onImportClick = { onImportItemClick(item.id) },
                                            onCancelClick = { onCancelItemClick(item.id) },
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

        @OptIn(ExperimentalTime::class)
        @Composable
        private fun ImportInfo(
            modifier: Modifier,
            import: ImportSession
        ) {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (import.inspection is Inspection.ContainerInspection.Webpage) {
                    Image(
                        modifier = Modifier.size(64.dp),
                        data = import.inspection.thumbnail?.let { Base64.decode(it) },
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Session: ${import.id}")
                    Text(import.uri)
                    Text(import.sourceType.name)
                    Text(Instant.fromEpochMilliseconds(import.creationDatetime).toString())
                    if (import.inspection is Inspection.ContainerInspection.Webpage) {
                        Text(import.inspection.title ?: "")
                        Text(import.inspection.description ?: "")
                    }
                }
            }
        }

        @Composable
        private fun ImportItem(
            modifier: Modifier,
            item: ImportSessionItem,
            onImportClick: () -> Unit,
            onCancelClick: () -> Unit,
            onRetryClick: () -> Unit
        ) {
            Item(
                modifier = modifier,
                contentPadding = PaddingValues(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (item.inspection) {
                        is Inspection.ItemInspection.InternetTrack -> {
                            Image(
                                modifier = Modifier.size(64.dp),
                                data = item.inspection.thumbnail?.let { Base64.decode(it) }
                            )
                        }
                        is Inspection.ItemInspection.LocalFileTrack -> {
                            Image(
                                modifier = Modifier.size(64.dp),
                                data = item.inspection.albumImage?.let { Base64.decode(it) }
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(item.inspection.title)
                        Text(
                            text = ""+item.inspection.album,
                            style = MaterialTheme.typography.labelMedium
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(item.inspection.artists) { artist ->
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
                            text = item.inspection.duration.formatted(),
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
                            // TODO: the media file may or may not exist at this point.
                            //       add a button to "show some details about it"; or if it was deleted, to "re-import it"
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