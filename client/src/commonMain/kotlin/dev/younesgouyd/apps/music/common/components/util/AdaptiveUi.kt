package dev.younesgouyd.apps.music.common.components.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

@Composable
fun AdaptiveUi(
    wide: @Composable () -> Unit,
    compact: @Composable () -> Unit
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val widthDp = with(density) { windowInfo.containerSize.width.toDp() }

    when {
        widthDp < 840.dp -> compact()
        else -> wide()
    }
}