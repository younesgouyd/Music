package dev.younesgouyd.apps.music.client.app.multiplatform.components.util

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import dev.younesgouyd.apps.music.client.app.multiplatform.MainActivity

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
actual fun getWindowSizeClass(): WindowWidthSizeClass {
    return calculateWindowSizeClass(MainActivity.instance /* TODO */).widthSizeClass
}

@Composable
actual fun SystemFolderPicker(
    onFolderChosen: (Uri) -> Unit, // TODO (rememberUpdatedState?)
    onCancelled: () -> Unit // TODO (rememberUpdatedState?)
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