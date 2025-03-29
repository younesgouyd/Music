package dev.younesgouyd.apps.music.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SplashScreen(
    private val repoStore: RepoStore,
    private val showContent: () -> Unit
) : Component() {
    override val title: String = ""
    private val message = MutableStateFlow("Loading")

    init {
        coroutineScope.launch {
            try {
                System.setProperty("sun.java2d.uiScale", "1.0")
                repoStore.init()
                showContent()
            } catch (e: Exception) {
                e.printStackTrace()
                message.value = "Something went wrong...\n\n${e.stackTraceToString()}"
            }
        }
    }

    @Composable
    override fun show(modifier: Modifier) {
        val message by message.collectAsState()

        MaterialTheme(
            content = {
                Surface(
                    modifier = modifier,
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(message)
                    }
                }
            }
        )
    }

    override fun clear() {
        coroutineScope.cancel()
    }
}