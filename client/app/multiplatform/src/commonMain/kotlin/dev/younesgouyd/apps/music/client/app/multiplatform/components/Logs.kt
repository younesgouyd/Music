package dev.younesgouyd.apps.music.client.app.multiplatform.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.app.multiplatform.components.util.Item
import dev.younesgouyd.apps.music.client.app.multiplatform.data.FileManager
import dev.younesgouyd.apps.music.client.app.multiplatform.util.Component
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Logs(
    val fileManager: FileManager
) : Component() {
    override val title: String = "Logs"
    private val state: MutableStateFlow<Ui.State> = MutableStateFlow(Ui.State.Loading)

    init {
        coroutineScope.launch {
            state.value = Ui.State.Loaded.Folder(
                name = fileManager.logsDir.name,
                files = withContext(Dispatchers.IO) { fileManager.logsDir.listFiles().orEmpty().toList().sortedByDescending { it.lastModified() } },
                isAtHome = true,
                onFileClick = ::onFileClick,
                onBackClick = {}
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

    private fun onFileClick(file: java.io.File) {
        coroutineScope.launch {
            state.value = if (file.isDirectory) {
                Ui.State.Loaded.Folder(
                    name = file.name,
                    files = withContext(Dispatchers.IO) { file.listFiles().orEmpty().toList().sortedByDescending { it.lastModified() } },
                    isAtHome = file == fileManager.logsDir,
                    onFileClick = ::onFileClick,
                    onBackClick = { onFileClick(file.parentFile) }
                )
            } else {
                Ui.State.Loaded.File(
                    name = file.name,
                    content = withContext(Dispatchers.IO) { file.readText() },
                    onBackClick = { onFileClick(file.parentFile) }
                )
            }
        }
    }

    private object Ui {
        sealed class State {
            data object Loading : State()

            sealed class Loaded: State() {
                data class Folder(
                    val name: String,
                    val files: List<java.io.File>,
                    val isAtHome: Boolean,
                    val onFileClick: (java.io.File) -> Unit,
                    val onBackClick: () -> Unit
                ) : Loaded()

                data class File(
                    val name: String,
                    val content: String,
                    val onBackClick: () -> Unit
                ) : Loaded()
            }
        }

        @Composable
        fun Main(
            modifier: Modifier,
            state: State
        ) {
            when (state) {
                is State.Loading -> Text(modifier = modifier, text = "Loading...")
                is State.Loaded -> Main(modifier = modifier, state = state)
            }
        }

        @Composable
        private fun Main(
            modifier: Modifier,
            state: State.Loaded
        ) {
            when(state) {
                is State.Loaded.Folder -> Folder(modifier, state)
                is State.Loaded.File -> File(modifier, state)
            }
        }

        @Composable
        private fun Folder(
            modifier: Modifier,
            state: State.Loaded.Folder
        ) {
            val name = state.name
            val files = state.files
            val isAtHome = state.isAtHome
            val onFileClick = state.onFileClick
            val onBackClick = state.onBackClick

            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (isAtHome) {
                        true -> Icon(Icons.Default.Home, null)
                        false -> IconButton(onBackClick) { Icon(Icons.AutoMirrored.Default.ArrowBack, null) }
                    }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.displayMedium,
                        minLines = 1,
                        maxLines = 1
                    )
                }
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files) { file ->
                        Item(
                            modifier = Modifier.width(400.dp),
                            contentPadding = PaddingValues(12.dp),
                            onClick = { onFileClick(file) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when (file.isDirectory) {
                                    true -> Icon(Icons.Default.Folder, null)
                                    false -> Icon(Icons.Default.FileOpen, null)
                                }
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun File(
            modifier: Modifier,
            state: State.Loaded.File
        ) {
            val name = state.name
            val content = state.content
            val onBackClick = state.onBackClick

            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onBackClick) { Icon(Icons.AutoMirrored.Default.ArrowBack, null) }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.displayMedium,
                        minLines = 1,
                        maxLines = 1
                    )
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    value = content,
                    readOnly = true,
                    onValueChange = {}
                )
            }
        }
    }
}