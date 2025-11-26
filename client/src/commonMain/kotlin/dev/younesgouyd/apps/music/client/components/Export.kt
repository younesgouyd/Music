package dev.younesgouyd.apps.music.client.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.client.Music
import dev.younesgouyd.apps.music.client.components.util.compose.SystemFolderPicker
import dev.younesgouyd.apps.music.client.usecases.ExportUseCase
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class Export(
    private val exportUseCase: ExportUseCase
) : Component() {
    override val title: String = "Export"
    val applicationScope = Music.coroutineScope
    var job: Job? = null
    private val inProgress = MutableStateFlow(false)

    @Composable
    override fun show(modifier: Modifier) {
        Ui.Main(
            modifier = modifier.fillMaxSize(),
            inProgress = inProgress.asStateFlow(),
            onFolderSelected = ::export,
            onCancelClick = ::cancel
        )
    }

    override fun clear() {
        job?.cancel()
        coroutineScope.cancel()
    }

    private fun export(destination: String) {
        if (job != null) {
            TODO()
        }
        job = applicationScope.launch {
            inProgress.value = true
            exportUseCase.execute(destination)
            inProgress.value = false
        }
    }

    private fun cancel() {
        job?.cancel()
        job = null
    }

    private object Ui {
        @Composable
        fun Main(
            modifier: Modifier,
            inProgress: StateFlow<Boolean>,
            onFolderSelected: (String) -> Unit,
            onCancelClick: () -> Unit
        ) {
            val inProgress by inProgress.collectAsState()
            var showSystemFilePicker by remember { mutableStateOf(false) }

            Surface(
                modifier = modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!inProgress) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showSystemFilePicker = true },
                            content = { Text("Select destination folder") }
                        )
                    } else {
                        Text("In progress...")
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onCancelClick,
                            content = { Text("Cancel") }
                        )
                    }
                }
            }

            if (showSystemFilePicker) {
                SystemFolderPicker(
                    onFolderChosen = {
                        showSystemFilePicker = false
                        onFolderSelected(it)
                    },
                    onCancelled = { showSystemFilePicker = false }
                )
            }
        }
    }
}
