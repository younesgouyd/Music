package dev.younesgouyd.apps.music.client.multiplatform.components.util

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.datamodels.Folder
import kotlinx.coroutines.flow.StateFlow

@Composable
fun Path(
    modifier: Modifier,
    path: List<Folder?>,
    searchQuery: StateFlow<String>,
    onFolderClick: (FolderId?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
) {
    val pathLazyListState = rememberLazyListState()
    val searchQuery by searchQuery.collectAsState()
    var isSearchVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(path) {
        if (path.isNotEmpty()) {
            pathLazyListState.animateScrollToItem(path.size - 1)
        }
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(TextFieldDefaults.MinHeight),
            contentAlignment = Alignment.Center
        ) {
            if (!isSearchVisible) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        state = pathLazyListState,
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(items = path) { folder ->
                            if (folder == null) {
                                Row(
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        content = { Text(".") },
                                        onClick = { onFolderClick(null) }
                                    )
                                    Text("/")
                                }
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        content = { Text(folder.name) },
                                        onClick = { onFolderClick(folder.id) }
                                    )
                                    Text("/")
                                }
                            }
                        }
                    }
                    IconButton(
                        onClick = { isSearchVisible = true },
                        content = { Icon(Icons.Default.Search, null) }
                    )
                }
            } else {
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                TextField(
                    modifier = Modifier.fillMaxWidth()
                        .focusRequester(focusRequester),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    label = { Text("Search") },
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    trailingIcon = {
                        IconButton(
                            onClick = { isSearchVisible = false; onSearchQueryChange("") },
                            content = { Icon(Icons.Default.Close, null) }
                        )
                    },
                    singleLine = true
                )
            }
        }
    }
}