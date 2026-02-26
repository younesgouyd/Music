package dev.younesgouyd.apps.music.client.app.multiplatform.components

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
import dev.younesgouyd.apps.music.client.app.multiplatform.util.Component
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow

class SplashScreen : Component() {
    override val title: String = ""
    private val message = MutableStateFlow("Loading")

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