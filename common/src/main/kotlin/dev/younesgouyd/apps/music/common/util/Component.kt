package dev.younesgouyd.apps.music.common.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

abstract class Component {
    protected val coroutineScope = CoroutineScope(SupervisorJob())
    abstract val title: String

    @Composable
    abstract fun show(modifier: Modifier)

    abstract fun clear()
}
