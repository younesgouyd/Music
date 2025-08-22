package dev.younesgouyd.apps.music.common.components.util

import androidx.compose.runtime.Composable

typealias Uri = String

@Composable
expect fun SystemFilePicker(onFolderPicked: (Uri) -> Unit)