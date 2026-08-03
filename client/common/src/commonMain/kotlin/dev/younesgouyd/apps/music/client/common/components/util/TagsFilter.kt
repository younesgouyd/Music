package dev.younesgouyd.apps.music.client.common.components.util

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.common.models.TagId
import kotlinx.coroutines.flow.StateFlow

@Composable
fun TagsFilter(
    modifier: Modifier,
    state: TagsFilterState
) {
    TagsFilter(
        modifier = modifier,
        tags = state.tags,
        tagSearchQuery = state.searchQuery,
        enableFiltering = state.enableFiltering,
        includeUntagged = state.includeUntagged,
        onTagSearchQueryChange = state.onSearchQueryChange,
        onEnableFilteringChange = state.onEnableFilteringChange,
        onIncludeUntaggedChange = state.onIncludeUntaggedChange,
        checkTag = state.checkTag,
        uncheckTag = state.uncheckTag
    )
}

@Composable
private fun TagsFilter(
    modifier: Modifier,
    tags: StateFlow<List<TagsFilterState.Tag>>,
    enableFiltering: StateFlow<Boolean>,
    includeUntagged: StateFlow<Boolean>,
    tagSearchQuery: StateFlow<String>,
    onTagSearchQueryChange: (String) -> Unit,
    onEnableFilteringChange: (Boolean) -> Unit,
    onIncludeUntaggedChange: (Boolean) -> Unit,
    checkTag: (TagId) -> Unit,
    uncheckTag: (TagId) -> Unit
) {
    val tags by tags.collectAsState()
    val tagSearchQuery by tagSearchQuery.collectAsState()
    val enableFiltering by enableFiltering.collectAsState()
    val includeUntagged by includeUntagged.collectAsState()
    var isSearchTagVisible by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 8.dp
    ) {
        LazyRow(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            stickyHeader {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = enableFiltering,
                            onCheckedChange = { onEnableFilteringChange(!enableFiltering) }
                        )
                        if (isSearchTagVisible) {
                            OutlinedTextField(
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                label = { Text("Search tags") },
                                value = tagSearchQuery,
                                maxLines = 1,
                                onValueChange = onTagSearchQueryChange,
                                trailingIcon = {
                                    IconButton(
                                        onClick = { isSearchTagVisible = false; onTagSearchQueryChange("") },
                                        content = { Icon(Icons.Default.Close, null) }
                                    )
                                }
                            )
                        } else {
                            IconButton(
                                onClick = { isSearchTagVisible = true },
                                content = { Icon(Icons.Default.Search, null) }
                            )
                        }
                        FilterChip(
                            leadingIcon = if (includeUntagged) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else { null },
                            label = { Text("Untagged") },
                            selected = includeUntagged,
                            onClick = { onIncludeUntaggedChange(!includeUntagged) }
                        )
                        VerticalDivider()
                    }
                }
            }
            items(tags) { tag ->
                FilterChip(
                    leadingIcon = if (tag.selected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    },
                    label = { Text(tag.name) },
                    selected = tag.selected,
                    onClick = { if (tag.selected) uncheckTag(tag.id) else checkTag(tag.id) }
                )
            }
        }
    }
}

data class TagsFilterState(
    val tags: StateFlow<List<Tag>>,
    val searchQuery: StateFlow<String>,
    val enableFiltering: StateFlow<Boolean>,
    val includeUntagged: StateFlow<Boolean>,
    val onSearchQueryChange: (String) -> Unit,
    val onEnableFilteringChange: (Boolean) -> Unit,
    val onIncludeUntaggedChange: (Boolean) -> Unit,
    val checkTag: (TagId) -> Unit,
    val uncheckTag: (TagId) -> Unit
) {
    data class Tag(
        val id: TagId,
        val name: String,
        val selected: Boolean
    )
}