package dev.younesgouyd.apps.music.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.common.components.util.MediaController
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow

abstract class Queue(
    protected val mediaController: MediaController
) : Component() {
    override val title: String = "Queue"
    protected val state: StateFlow<MediaController.MediaControllerState>
        get() = mediaController.state

    @Composable
    abstract override fun show(modifier: Modifier)

    override fun clear() {
        coroutineScope.cancel()
    }
}
