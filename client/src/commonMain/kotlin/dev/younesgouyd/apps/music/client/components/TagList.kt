package dev.younesgouyd.apps.music.client.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.younesgouyd.apps.music.client.components.util.Item
import dev.younesgouyd.apps.music.client.components.util.ScrollToTopFloatingActionButton
import dev.younesgouyd.apps.music.client.data.TagId
import dev.younesgouyd.apps.music.client.data.repoes.TagRepo
import dev.younesgouyd.apps.music.client.data.room.entities.Tag
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TagList(
    tagRepo: TagRepo,
    showTag: (TagId) -> Unit
) : Component() {
    override val title: String = "Tags"
    private val searchQuery = MutableStateFlow("")
    private val state: MutableStateFlow<TagListState> = MutableStateFlow(TagListState.Loading)

    init {
        coroutineScope.launch {
            state.value = TagListState.Loaded(
                scrollState = LazyListState(),
                searchQuery = searchQuery.asStateFlow(),
                tags = searchQuery.flatMapLatest { tagRepo.search(it) }.stateIn(coroutineScope),
                onSearchQueryChange = { searchQuery.value = it },
                onAddTag = { name: String, description: String? ->
                    coroutineScope.launch {
                        tagRepo.add(name = name, description = description)
                    }
                },
                onTagClick = showTag,
                onDeleteTagClick = {
                    coroutineScope.launch {
                        tagRepo.delete(it)
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

    private sealed class TagListState {
        data object Loading : TagListState()

        data class Loaded(
            val scrollState: LazyListState,
            val searchQuery: StateFlow<String>,
            val tags: StateFlow<List<Tag>>,
            val onSearchQueryChange: (String) -> Unit,
            val onAddTag: (name: String, description: String?) -> Unit,
            val onTagClick: (TagId) -> Unit,
            val onDeleteTagClick: (TagId) -> Unit
        ) : TagListState()
    }

    private object Ui {
        @Composable
        fun Main(
            modifier: Modifier,
            state: TagListState
        ) {
            when (state) {
                is TagListState.Loading -> Text(modifier = modifier, text = "Loading...")
                is TagListState.Loaded -> Main(modifier = modifier, loaded = state)
            }
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            loaded: TagListState.Loaded
        ) {
            Main(
                modifier = modifier,
                scrollState = loaded.scrollState,
                searchQuery = loaded.searchQuery,
                tags = loaded.tags,
                onSearchQueryChange = loaded.onSearchQueryChange,
                onAddTag = loaded.onAddTag,
                onTagClick = loaded.onTagClick,
                onDeleteTagClick = loaded.onDeleteTagClick
            )
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            scrollState: LazyListState,
            searchQuery: StateFlow<String>,
            tags: StateFlow<List<Tag>>,
            onSearchQueryChange: (String) -> Unit,
            onAddTag: (name: String, description: String?) -> Unit,
            onTagClick: (TagId) -> Unit,
            onDeleteTagClick: (TagId) -> Unit
        ) {
            val searchQuery by searchQuery.collectAsState()
            val tags by tags.collectAsState()
            var isNewTagDialogVisible by remember { mutableStateOf(false) }

            Scaffold(
                modifier = modifier.fillMaxSize(),
                content = { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                            state = scrollState,
                            contentPadding = PaddingValues(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            stickyHeader {
                                Surface {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            modifier = Modifier.weight(1f),
                                            leadingIcon = { Icon(Icons.Default.Search, null) },
                                            label = { Text("Search") },
                                            value = searchQuery,
                                            onValueChange = onSearchQueryChange
                                        )
                                        Button(
                                            content = { Text("Add new tag") },
                                            onClick = { if (!isNewTagDialogVisible) isNewTagDialogVisible = true }
                                        )
                                    }
                                }
                            }
                            items(items = tags, key = { it.id.value }) { tag ->
                                TagItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    tag = tag,
                                    onClick = { onTagClick(tag.id) },
                                    onDeleteClick = { onDeleteTagClick(tag.id) },
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    ScrollToTopFloatingActionButton(scrollState)
                }
            )

            if (isNewTagDialogVisible) {
                NewTagDialog(
                    onDismiss = { isNewTagDialogVisible = false },
                    onDone = { name: String, description: String? ->
                        onAddTag(name, description)
                        isNewTagDialogVisible = false
                    }
                )
            }
        }

        @Composable
        private fun NewTagDialog(
            onDismiss: () -> Unit,
            onDone: (name: String, description: String?) -> Unit
        ) {
            var name by remember { mutableStateOf("") }
            var description by remember { mutableStateOf("") }
            var invalidName by remember { mutableStateOf(false) }

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
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Name") },
                            value = name,
                            onValueChange = {
                                name = it
                                invalidName = it.isBlank()
                            },
                            singleLine = true,
                            isError = invalidName,
                            supportingText = if (invalidName) {
                                { Text("Invalid input") }
                            } else null
                        )
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Description") },
                            value = description,
                            onValueChange = { description = it }
                        )
                        Button(
                            content = { Text("Done") },
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (name.isBlank()) {
                                    invalidName = true
                                } else {
                                    onDone(name, description)
                                }
                            }
                        )
                    }
                }
            }
        }

        @Composable
        private fun TagItem(
            modifier: Modifier,
            tag: Tag,
            onClick: () -> Unit,
            onDeleteClick: () -> Unit
        ) {
            Item(
                modifier = modifier,
                onClick = onClick
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tag.name)
                    IconButton(
                        content = { Icon(Icons.Default.Delete, null) },
                        onClick = onDeleteClick
                    )
                }
            }
        }
    }
}