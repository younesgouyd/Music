package dev.younesgouyd.apps.music.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

typealias ComponentId = String

abstract class Component {
    companion object {
        private var nextId = 0
    }
    protected val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    abstract val title: String
    val id: ComponentId = (nextId++).toString()

    @Composable
    abstract fun show(modifier: Modifier)

    abstract fun clear()
}
