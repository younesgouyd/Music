package dev.younesgouyd.apps.music.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.*
import dev.younesgouyd.apps.music.app.components.Main
import dev.younesgouyd.apps.music.app.components.SplashScreen
import dev.younesgouyd.apps.music.app.data.RepoStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

object Application {
    private val repoStore = RepoStore()
    private val currentComponent: MutableStateFlow<Component>

    init {
        currentComponent = MutableStateFlow(
            SplashScreen(
                repoStore = repoStore,
                showContent = ::showContent
            )
        )
    }

    fun start() {
        application {
            val currentComponent by currentComponent.collectAsState()

            Window(
                state = rememberWindowState(
                    placement = WindowPlacement.Maximized,
                    position = WindowPosition(Alignment.Center)
                ),
                onCloseRequest = { currentComponent.clear(); exitApplication() },
                content = { currentComponent.show(Modifier.fillMaxSize()) }
            )
        }
    }

    private fun showContent() {
        currentComponent.update {
            it.clear()
            Main(repoStore)
        }
    }
}