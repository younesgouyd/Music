package dev.younesgouyd.apps.music.client.multiplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.multiplatform.components.util.LazilyLoadedItems
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.ImportSessionRepo
import dev.younesgouyd.apps.music.common.models.ImportSessionId
import dev.younesgouyd.apps.music.common.models.Offset
import dev.younesgouyd.apps.music.common.models.datamodels.ImportSession
import dev.younesgouyd.apps.music.common.ui.Component
import dev.younesgouyd.apps.music.common.ui.Item
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

class ImportList(
    importSessionRepo: ImportSessionRepo,
    showImportDetails: (ImportSessionId) -> Unit
) : Component() {
    override val title: String = "Imports"
    private val state = Ui.State(
        imports = LazilyLoadedItems(
            scope = coroutineScope,
            loader = { offset, limit ->
                val rows = importSessionRepo.getAll(limit, offset)
                LazilyLoadedItems.Page(
                    nextOffset = if (rows.size < limit) { null } else { Offset.Index(offset.value + limit) },
                    items = rows
                )
            },
            initialOffset = Offset.Index.initial()
        ),
        onItemClick = showImportDetails
    )

    @Composable
    override fun show(modifier: Modifier) {
        Ui.Main(modifier, state)
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private object Ui {
        class State(
            val imports: LazilyLoadedItems<ImportSession, Offset.Index>,
            val onItemClick: (ImportSessionId) -> Unit
        )

        @Composable
        fun Main(modifier: Modifier = Modifier, state: State) {
            Main(
                modifier = modifier,
                imports = state.imports,
                onItemClick = state.onItemClick
            )
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            imports: LazilyLoadedItems<ImportSession, Offset.Index>,
            onItemClick: (ImportSessionId) -> Unit
        ) {
            val items by imports.items.collectAsState()
            val loadingItems by imports.loading.collectAsState()
            val scrollState = rememberLazyListState()

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

            LaunchedEffect(scrollState, imports) {
                snapshotFlow {
                    scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                }.map { it == null ||  it >= items.size - 5  }
                    .distinctUntilChanged()
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