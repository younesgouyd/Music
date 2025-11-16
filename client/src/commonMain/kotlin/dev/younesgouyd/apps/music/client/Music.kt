package dev.younesgouyd.apps.music.client

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.client.data.RepoStore
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

abstract class Music {
    companion object {
        val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    protected abstract val appDir: File
    protected abstract val repoStore: RepoStore
    protected abstract val currentComponent: MutableStateFlow<Component>

    @Composable
    fun show(modifier: Modifier) {
        val currentComponent by currentComponent.collectAsState()

        currentComponent.show(modifier.fillMaxSize())
    }

    fun clear() {
        currentComponent.value.clear()
        coroutineScope.cancel()
    }

    protected abstract fun showContent()
}

expect class MusicImpl : Music