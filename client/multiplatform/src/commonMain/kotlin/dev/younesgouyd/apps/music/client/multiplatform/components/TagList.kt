package dev.younesgouyd.apps.music.client.multiplatform.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.younesgouyd.apps.music.client.multiplatform.components.util.DeleteConfirmationDialog
import dev.younesgouyd.apps.music.client.multiplatform.components.util.ScrollToTopFloatingActionButton
import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.TagRepo
import dev.younesgouyd.apps.music.common.models.TagId
import dev.younesgouyd.apps.music.common.models.datamodels.Tag
import dev.younesgouyd.apps.music.common.ui.Component
import dev.younesgouyd.apps.music.common.ui.Item
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
    private val state: MutableStateFlow<TagListState> = MutableStateFlow(TagListState.Loading)

    init {
        val searchQuery = MutableStateFlow("")
        coroutineScope.launch {
            state.value = TagListState.Loaded(
                scrollState = ScrollState(0),
                searchQuery = searchQuery.asStateFlow(),
                tags = searchQuery.flatMapLatest { tagRepo.search(it) }.stateIn(coroutineScope),
                onSearchQueryChange = { searchQuery.value = it },
                onAddTag = { name: String ->
                    coroutineScope.launch {
                        tagRepo.add(
                            name = name
                                .trim() // TODO
                        )
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

        class Loaded(
            val scrollState: ScrollState,
            val searchQuery: StateFlow<String>,
            val tags: StateFlow<List<Tag>>,
            val onSearchQueryChange: (String) -> Unit,
            val onAddTag: (name: String) -> Unit,
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
            scrollState: ScrollState,
            searchQuery: StateFlow<String>,
            tags: StateFlow<List<Tag>>,
            onSearchQueryChange: (String) -> Unit,
            onAddTag: (name: String) -> Unit,
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
                        Column(
                            modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
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
                            FlowRow(
                                modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                itemVerticalAlignment = Alignment.CenterVertically
                            ) {
                                for (tag in tags) {
                                    TagItem(
                                        tag = tag,
                                        onClick = { onTagClick(tag.id) },
                                        onDeleteClick = { onDeleteTagClick(tag.id) },
                                    )
                                }
                            }
                        }
                    }
                },
                floatingActionButton = { ScrollToTopFloatingActionButton(scrollState) }
            )

            if (isNewTagDialogVisible) {
                NewTagDialog(
                    onDismiss = { isNewTagDialogVisible = false },
                    onDone = { name: String ->
                        onAddTag(name)
                        isNewTagDialogVisible = false
                    }
                )
            }
        }

        @Composable
        private fun NewTagDialog(
            onDismiss: () -> Unit,
            onDone: (name: String) -> Unit
        ) {
            var name by remember { mutableStateOf("") }
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
                        Button(
                            content = { Text("Done") },
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (name.isBlank()) {
                                    invalidName = true
                                } else {
                                    onDone(name)
                                }
                            }
                        )
                    }
                }
            }
        }

        @Composable
        private fun TagItem(
            tag: Tag,
            onClick: () -> Unit,
            onDeleteClick: () -> Unit
        ) {
            var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

            Item(
                contentPadding = PaddingValues(8.dp),
                onClick = onClick,
                onLongClick = { showDeleteConfirmationDialog = true }
            ) {
                Text(tag.name)
            }
            if (showDeleteConfirmationDialog) {
                DeleteConfirmationDialog(
                    message = "Delete tag \"${tag.name}\"? it will be removed from all tracks.",
                    onDismissRequest = { showDeleteConfirmationDialog = false },
                    onYesClick = {
                        showDeleteConfirmationDialog = false
                        onDeleteClick()
                    }
                )
            }
        }
    }
}