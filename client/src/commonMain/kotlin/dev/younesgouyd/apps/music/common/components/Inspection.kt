package dev.younesgouyd.apps.music.common.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.common.Inspection
import dev.younesgouyd.apps.music.common.components.util.widgets.Image
import dev.younesgouyd.apps.music.common.components.util.widgets.Item
import dev.younesgouyd.apps.music.common.data.Server
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class Inspection(
    private val server: Server,
    private val onDone: (Map<Long, String>) -> Unit,
    url: String
) : Component() {
    override val title: String = "Inspection"
    private val state: MutableStateFlow<InspectionState> = MutableStateFlow(InspectionState.Loading)
    private val selectedItems = mutableStateListOf<Long>()

    init {
        coroutineScope.launch {
            state.value = try {
                val inspection = server.inspect(url)
                InspectionState.Loaded(
                    inspection = inspection,
                    selectedItems = selectedItems,
                    onItemClick = { itemId ->
                        val exists = selectedItems.any { it == itemId }
                        if (exists) {
                            selectedItems.remove(itemId)
                        } else {
                            selectedItems.add(itemId)
                        }
                    },
                    onSelectAllClick = {
                        selectedItems.clear()
                        selectedItems.addAll(inspection.items.map { it.id })
                    },
                    onUnselectAllClick = { selectedItems.clear() },
                    onDone = {
                        onDone(
                            selectedItems.associateWith { selectedId ->
                                inspection.items.find { it.id == selectedId }!!.url
                            }
                        )
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                InspectionState.Error
            }
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

    private sealed class InspectionState {
        data object Loading : InspectionState()

        data class Loaded(
            val inspection: Inspection,
            val selectedItems: List<Long>,
            val onItemClick: (Long) -> Unit,
            val onSelectAllClick: () -> Unit,
            val onUnselectAllClick: () -> Unit,
            val onDone: () -> Unit
        ) : InspectionState()

        data object Error : InspectionState()
    }

    private object Ui {
        @Composable
        fun Main(
            modifier: Modifier,
            state: InspectionState
        ) {
            Surface {
                when (state) {
                    is InspectionState.Loading -> Text(modifier = modifier, text = "Loading...")
                    is InspectionState.Loaded -> Main(modifier = modifier, state = state)
                    is InspectionState.Error -> Text(modifier = modifier, text = "Something went wrong.")
                }
            }
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            state: InspectionState.Loaded
        ) {
            Main(
                modifier = modifier,
                items = state.inspection.items,
                selectedItems = state.selectedItems,
                onItemClick = state.onItemClick,
                onSelectAllClick = state.onSelectAllClick,
                onUnselectAllClick = state.onUnselectAllClick,
                onDone = state.onDone
            )
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            items: List<Inspection.Item>,
            selectedItems: List<Long>,
            onItemClick: (Long) -> Unit,
            onSelectAllClick: () -> Unit,
            onUnselectAllClick: () -> Unit,
            onDone: () -> Unit
        ) {
            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.background
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stickyHeader {
                        Surface {
                            Column {
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = onDone,
                                    content = { Text("Done") }
                                )
                                TextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        if (selectedItems.size == items.size) {
                                            onUnselectAllClick()
                                        } else {
                                            onSelectAllClick()
                                        }
                                    },
                                    content = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = selectedItems.size == items.size,
                                                onCheckedChange = null
                                            )
                                            Text("Select all")
                                        }
                                    }
                                )
                            }
                        }
                    }
                    items(items = items) { item ->
                        Item(
                            modifier = Modifier.fillMaxWidth(),
                            item = item,
                            selected = selectedItems.any { it == item.id },
                            onClick = { onItemClick(item.id) }
                        )
                    }
                }
            }
        }

        @Composable
        private fun Item(
            modifier: Modifier,
            item: Inspection.Item,
            selected: Boolean,
            onClick: () -> Unit
        ) {
            Item(
                modifier = modifier,
                onClick = onClick,
                contentPadding = PaddingValues(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = null
                    )
                    Image(
                        modifier = Modifier.size(64.dp),
                        url = item.thumbnail
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(item.title)
                        Text(
                            text = ""+item.album,
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
                            text = ""+item.duration?.seconds?.toComponents { minutes: Long, seconds: Int, _: Int -> "$minutes:$seconds" },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}