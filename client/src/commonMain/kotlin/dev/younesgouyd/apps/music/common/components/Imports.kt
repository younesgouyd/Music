package dev.younesgouyd.apps.music.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.common.components.util.compose.widgets.Item
import dev.younesgouyd.apps.music.common.data.repoes.ImportSessionRepo
import dev.younesgouyd.apps.music.common.data.room.entities.ImportSessionWithItems
import dev.younesgouyd.apps.music.common.util.Component
import dev.younesgouyd.apps.music.common.util.ImportSessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class Imports(
    importSessionRepo: ImportSessionRepo
) : Component() {
    override val title: String = "Import Status"
    private val state: MutableStateFlow<ImportsState> = MutableStateFlow(ImportsState.Loading)

    init {
        val customOrder = mapOf(
            ImportSessionState.Started to 0,
            ImportSessionState.Pending to 1,
            ImportSessionState.Completed to 2,
            ImportSessionState.Cancelled to 2,
            ImportSessionState.Failed to 2
        )
        coroutineScope.launch {
            state.value = ImportsState.Loaded(
                imports = importSessionRepo.getAll()
                    .mapLatest { list ->
                        list.sortedWith { a, b ->
                            // first sort by state
                            val orderCmp = customOrder[a.importSession.state]!!.compareTo(customOrder[b.importSession.state]!!)
                            if (orderCmp != 0) return@sortedWith orderCmp

                            // then by creationDatetime
                            return@sortedWith when (a.importSession.state) {
                                ImportSessionState.Pending -> a.importSession.creationDatetime.compareTo(b.importSession.creationDatetime) // ASC
                                else -> b.importSession.creationDatetime.compareTo(a.importSession.creationDatetime) // DESC
                            }
                        }
                    }.stateIn(coroutineScope),
                scrollState = LazyListState(),
                onCancelImportClick = {
                    coroutineScope.launch {
                        importSessionRepo.updateState(it, ImportSessionState.Cancelled)
                    }
                },
                onDeleteImportClick = {
                    // TODO: don't allow started imports to be deleted. they should be cancelled first.
                    coroutineScope.launch {
                        importSessionRepo.delete(it)
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

    private sealed class ImportsState {
        data object Loading : ImportsState()

        data class Loaded(
            val imports: StateFlow<List<ImportSessionWithItems>>,
            val scrollState: LazyListState,
            val onCancelImportClick: (Long) -> Unit,
            val onDeleteImportClick: (Long) -> Unit
        ) : ImportsState()
    }

    private object Ui {
        @Composable
        fun Main(modifier: Modifier = Modifier, state: ImportsState) {
            when (state) {
                is ImportsState.Loading -> Text(modifier = modifier, text = "Loading...")
                is ImportsState.Loaded -> Main(modifier = modifier, state = state)
            }
        }

        @Composable
        private fun Main(modifier: Modifier, state: ImportsState.Loaded) {
            Main(
                modifier = modifier,
                imports = state.imports,
                scrollState = state.scrollState,
                onCancelImportClick = state.onCancelImportClick,
                onDeleteImportClick = state.onDeleteImportClick
            )
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            imports: StateFlow<List<ImportSessionWithItems>>,
            scrollState: LazyListState,
            onCancelImportClick: (Long) -> Unit,
            onDeleteImportClick: (Long) -> Unit
        ) {
            val imports by imports.collectAsState()

            Surface(
                modifier = modifier
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(8.dp),
                    state = scrollState
                ) {
                    items(
                        items = imports
                    ) { item ->
                        ImportItem(
                            modifier = Modifier.fillMaxWidth(),
                            import = item,
                            onCancelClick = { onCancelImportClick(item.importSession.id) },
                            onDeleteClick = { onDeleteImportClick(item.importSession.id) }
                        )
                    }
                }
            }
        }

        @Composable
        private fun ImportItem(
            modifier: Modifier = Modifier,
            import: ImportSessionWithItems,
            onCancelClick: () -> Unit,
            onDeleteClick: () -> Unit
        ) {
            Item(
                modifier = modifier,
                contentPadding = PaddingValues(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Session: ${import.importSession.id}")
                    Text(import.importSession.uri)
                    Text(import.importSession.sourceType.name)
                    Text(
                        text = import.importSession.state.name,
                        color = when (import.importSession.state) {
                            ImportSessionState.Pending -> Color.Unspecified
                            ImportSessionState.Started -> Color.Unspecified
                            ImportSessionState.Completed -> Color(0xFF4CAF50)
                            ImportSessionState.Cancelled -> Color.Unspecified
                            ImportSessionState.Failed -> MaterialTheme.colorScheme.error
                        }
                    )
                    Text(Instant.fromEpochMilliseconds(import.importSession.creationDatetime).toString())
                    if (import.importSession.state == ImportSessionState.Started) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (import.importSession.state == ImportSessionState.Started) {
                            TextButton(
                                onClick = onCancelClick,
                                content = { Text("Cancel") }
                            )
                        } else {
                            TextButton(
                                onClick = onDeleteClick,
                                content = { Text("Delete") }
                            )
                        }
                    }
                }
            }
        }
    }
}