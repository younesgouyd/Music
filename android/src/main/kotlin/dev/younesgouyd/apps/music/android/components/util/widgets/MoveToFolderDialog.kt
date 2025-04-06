package dev.younesgouyd.apps.music.android.components.util.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.younesgouyd.apps.music.common.data.sqldelight.migrations.Folder
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class ItemToMove(
    val id: Long,
    val name: String,
    val image: ByteArray?,
    val typeLabel: String
)

@Composable
fun MoveToFolderDialog(
    itemToMove: ItemToMove,
    loadFolders: suspend (parentFolderId: Long?) -> List<Folder>,
    onMoveToFolder: (id: Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var path: Set<Folder?> by remember { mutableStateOf(setOf(null)) }
    val pathLazyListState = rememberLazyListState()
    val folders: SnapshotStateList<Folder> = remember { mutableStateListOf() }
    val lazyColumnState = rememberLazyListState()
    var currentCollection: Job? by remember { mutableStateOf<Job?>(null) }

    fun onFolderClick(folder: Folder?) {
        currentCollection?.cancel()
        currentCollection = coroutineScope.launch {
            folders.clear()
            folders.addAll(loadFolders(folder?.id))
        }
        coroutineScope.launch {
            val list = path.drop(1)
                .takeWhile { folder != null && it!!.id != folder.id}
                .toMutableList()
            list.add(0, null)
            list.add(folder)
            path = list.toSet()
            pathLazyListState.animateScrollToItem(path.indices.last)
        }
    }

    LaunchedEffect(Unit) {
        currentCollection?.cancel()
        currentCollection = launch {
            folders.clear()
            folders.addAll(loadFolders(null))
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.size(width = 500.dp, height = 600.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    text = "Add to folder",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier.size(64.dp),
                        data = itemToMove.image
                    )
                    Text(
                        text = itemToMove.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        state = pathLazyListState,
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
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
                        }
                        items(items = path.drop(1), key = { it!!.id }) { folder ->
                            require(folder != null)
                            Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    content = { Text(folder.name) },
                                    onClick = { onFolderClick(folder) }
                                )
                                Text("/")
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                        state = lazyColumnState,
                        contentPadding = PaddingValues(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = folders,
                            key = { it.id }
                        ) { folderOption ->
                            Item(
                                modifier = Modifier.padding(8.dp),
                                onClick = { onMoveToFolder(folderOption.id) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        modifier = Modifier.size(64.dp).aspectRatio(1f),
                                        imageVector = Icons.Default.Folder,
                                        contentScale = ContentScale.FillWidth,
                                        alignment = Alignment.TopCenter,
                                        contentDescription = null
                                    )
                                    Text(
                                        text = folderOption.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        content = { Icon(Icons.AutoMirrored.Default.ArrowForward, null) },
                                        onClick = { onFolderClick(folderOption) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}