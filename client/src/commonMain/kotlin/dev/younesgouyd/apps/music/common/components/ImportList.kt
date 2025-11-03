package dev.younesgouyd.apps.music.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.common.components.util.compose.widgets.Item
import dev.younesgouyd.apps.music.common.data.repoes.ImportSessionRepo
import dev.younesgouyd.apps.music.common.data.room.entities.ImportSession
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class ImportList(
    importSessionRepo: ImportSessionRepo,
    showImportDetails: (Long) -> Unit
) : Component() {
    override val title: String = "Imports"
    private val state: MutableStateFlow<ImportListState> = MutableStateFlow(ImportListState.Loading)

    init {
        coroutineScope.launch {
            state.value = ImportListState.Loaded(
                imports = importSessionRepo.getAll().stateIn(coroutineScope),
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

    private sealed class ImportListState {
        data object Loading : ImportListState()

        data class Loaded(
            val imports: StateFlow<List<ImportSession>>,
            val scrollState: LazyListState,
            val onItemClick: (Long) -> Unit
        ) : ImportListState()
    }

    private object Ui {
        @Composable
        fun Main(modifier: Modifier = Modifier, state: ImportListState) {
            when (state) {
                is ImportListState.Loading -> Text(modifier = modifier, text = "Loading...")
                is ImportListState.Loaded -> Main(modifier = modifier, state = state)
            }
        }

        @Composable
        private fun Main(modifier: Modifier, state: ImportListState.Loaded) {
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
            imports: StateFlow<List<ImportSession>>,
            scrollState: LazyListState,
            onItemClick: (Long) -> Unit
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
                            onClick = { onItemClick(item.id) }
                        )
                    }
                }
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
                    Text(import.sourceType.name)
                    Text(Instant.fromEpochMilliseconds(import.creationDatetime).toString())
                }
            }
        }
    }
}