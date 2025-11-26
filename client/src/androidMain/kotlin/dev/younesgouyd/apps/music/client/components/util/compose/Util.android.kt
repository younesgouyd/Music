package dev.younesgouyd.apps.music.client.components.util.compose

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun SystemFolderPicker(
    onFolderChosen: (Uri) -> Unit,
    onCancelled: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            onFolderChosen(uri.toString())
        } else {
            onCancelled()
        }
    }
    LaunchedEffect(Unit) {
        launcher.launch(null)
    }
}

@Composable
actual fun SystemFilePicker(
    onFileChosen: (Uri) -> Unit,
    onCancelled: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            onFileChosen(uri.toString())
        } else {
            onCancelled()
        }
    }
    LaunchedEffect(Unit) {
        launcher.launch(arrayOf("application/zip")) // TODO
    }
}