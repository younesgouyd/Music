package dev.younesgouyd.apps.music.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.*
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.data.sqldelight.YounesMusic
import dev.younesgouyd.apps.music.common.util.Component
import dev.younesgouyd.apps.music.desktop.components.Main
import dev.younesgouyd.apps.music.desktop.components.SplashScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.*

object Application {
    private val repoStore: RepoStore
    private val currentComponent: MutableStateFlow<Component>

    init {
        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:younesmusic.db",
            properties = Properties().apply { put("foreign_keys", "true") }
        )
        val file = File("younesmusic.db")
        if (!file.exists()) {
            file.createNewFile()
            YounesMusic.Schema.create(driver)
        }
        repoStore = RepoStore(YounesMusic(driver), FileManager())
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
            Main(
                repoStore = repoStore,
                mediaPlayer = MediaPlayer(),
                mediaUtil = MediaUtil()
            )
        }
    }
}