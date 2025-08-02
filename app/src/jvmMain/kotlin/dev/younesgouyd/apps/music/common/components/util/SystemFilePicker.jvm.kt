package dev.younesgouyd.apps.music.common.components.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import javax.swing.JFileChooser

@Composable
actual fun SystemFilePicker(onFolderPicked: (Uri) -> Unit) {
    LaunchedEffect(Unit) {
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Choose a folder"
        }
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            onFolderPicked(chooser.selectedFile.toURI().toString())
        }
    }
}