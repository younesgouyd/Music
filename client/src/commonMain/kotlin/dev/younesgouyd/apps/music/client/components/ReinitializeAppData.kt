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
import dev.younesgouyd.apps.music.client.components.util.compose.SystemFilePicker
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReinitializeAppData(
    private val execute: (sourceFileUri: String) -> Unit
) : Component() {
    override val title: String = "Reinitialize App Data"
    private val inProgress = MutableStateFlow(false)

    @Composable
    override fun show(modifier: Modifier) {
        Ui.Main(
            modifier = modifier.fillMaxSize(),
            inProgress = inProgress.asStateFlow(),
            onFileSelected = ::start
        )
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private fun start(sourceFileUri: String) {
        inProgress.value = true
        execute(sourceFileUri)
    }

    private object Ui {
        @Composable
        fun Main(
            modifier: Modifier,
            inProgress: StateFlow<Boolean>,
            onFileSelected: (String) -> Unit
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
                            content = { Text("Select data file") }
                        )
                    } else {
                        Text("In progress...")
                    }
                }
            }

            if (showSystemFilePicker) {
                SystemFilePicker(
                    onFileChosen = {
                        showSystemFilePicker = false
                        onFileSelected(it)
                    },
                    onCancelled = { showSystemFilePicker = false }
                )
            }
        }
    }
}