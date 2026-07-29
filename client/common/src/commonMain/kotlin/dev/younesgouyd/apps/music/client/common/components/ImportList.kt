package dev.younesgouyd.apps.music.client.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.common.components.util.Item
import dev.younesgouyd.apps.music.client.common.data.repoes.ImportSessionRepo
import dev.younesgouyd.apps.music.client.common.util.Component
import dev.younesgouyd.apps.music.client.common.util.LazilyLoadedItems
import dev.younesgouyd.apps.music.common.ImportSession
import dev.younesgouyd.apps.music.common.ImportSessionId
import dev.younesgouyd.apps.music.common.Offset
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Instant

class ImportList(
    importSessionRepo: ImportSessionRepo,
    showImportDetails: (ImportSessionId) -> Unit
) : Component() {
    override val title: String = "Imports"
    private val state: MutableStateFlow<Ui.State> = MutableStateFlow(Ui.State.Loading)

    init {
        coroutineScope.launch {
            state.value = Ui.State.Loaded(
                imports = LazilyLoadedItems(
                    coroutineScope = coroutineScope,
                    load = { offset, limit ->
                        val rows = importSessionRepo.getAll(limit, offset)
                        LazilyLoadedItems.Page(
                            nextOffset = if (rows.size < limit) {
                                null
                            } else {
                                Offset.Index(
                                    offset.value + limit
                                )
                            },
                            items = rows
                        )
                    },
                    initialOffset = Offset.Index.initial()
                ),
                scrollState = LazyListState(),
                onItemClick = showImportDetails
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
                val imports: LazilyLoadedItems<ImportSession, Offset.Index>,
                val scrollState: LazyListState,
                val onItemClick: (ImportSessionId) -> Unit
            ) : State()
        }

        @Composable
        fun Main(modifier: Modifier = Modifier, state: State) {
            when (state) {
                is State.Loading -> Text(modifier = modifier, text = "Loading...")
                is State.Loaded -> Main(modifier = modifier, state = state)
            }
        }

        @Composable
        private fun Main(modifier: Modifier, state: State.Loaded) {
            Main(
                modifier = modifier,
                imports = state.imports,
                scrollState = state.scrollState,
                onItemClick = state.onItemClick
            )
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            imports: LazilyLoadedItems<ImportSession, Offset.Index>,
            scrollState: LazyListState,
            onItemClick: (ImportSessionId) -> Unit
        ) {
            val items by imports.items.collectAsState()
            val loadingItems by imports.loading.collectAsState()

            Surface(
                modifier = modifier
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    state = scrollState,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(
                        items = items
                    ) { item ->
                        ImportItem(
                            modifier = Modifier.fillMaxWidth(),
                            import = item,
                            onClick = { onItemClick(item.id) }
                        )
                    }
                    if (loadingItems) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(50.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }

            LaunchedEffect(scrollState) {
                snapshotFlow {
                    scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                }.map { it == null ||  it >= items.size - 5  }
                    .filter { it }
                    .collect { imports.loadMore() }
            }
        }

        @Composable
        private fun ImportItem(
            modifier: Modifier,
            import: ImportSession,
            onClick: () -> Unit
        ) {
            Item(
                modifier = modifier,
                contentPadding = PaddingValues(8.dp),
                onClick = onClick
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Session: ${import.id}")
                    Text(import.uri)
                    Text(Instant.fromEpochMilliseconds(import.creationDatetime).toString())
                }
            }
        }
    }
}