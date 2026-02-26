package dev.younesgouyd.apps.music.client.app.multiplatform.components.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import javax.swing.JFileChooser

@Composable
actual fun SystemFolderPicker(
    onFolderChosen: (Uri) -> Unit,
    onCancelled: () -> Unit
) {
    LaunchedEffect(Unit) {
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Choose a folder"
        }
        val result = chooser.showOpenDialog(null)
        when (result) {
            JFileChooser.APPROVE_OPTION -> onFolderChosen(chooser.selectedFile.toURI().toString())
            JFileChooser.CANCEL_OPTION -> onCancelled()
            else -> TODO()
        }
    }
}

@Composable
actual fun SystemFilePicker(
    onFileChosen: (Uri) -> Unit,
    onCancelled: () -> Unit
) {
    LaunchedEffect(Unit) {
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.FILES_ONLY
            dialogTitle = "Choose a file"
        }
        val result = chooser.showOpenDialog(null)
        when (result) {
            JFileChooser.APPROVE_OPTION -> onFileChosen(chooser.selectedFile.toURI().toString())
            JFileChooser.CANCEL_OPTION -> onCancelled()
            else -> TODO()
        }
    }
}