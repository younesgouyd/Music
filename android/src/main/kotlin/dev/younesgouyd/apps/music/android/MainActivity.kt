package dev.younesgouyd.apps.music.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.younesgouyd.apps.music.android.components.Main
import dev.younesgouyd.apps.music.android.components.SplashScreen
import dev.younesgouyd.apps.music.common.data.RepoStore
import dev.younesgouyd.apps.music.common.data.sqldelight.YounesMusic
import dev.younesgouyd.apps.music.common.util.Component
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class MainActivity : ComponentActivity() {
    private lateinit var _repoStore: RepoStore
    val repoStore by lazy { _repoStore }
    private lateinit var currentComponent: MutableStateFlow<Component>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val driver = AndroidSqliteDriver(schema = YounesMusic.Schema, context = this, "younesmusic.db")
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        _repoStore = RepoStore(
            database = YounesMusic(driver),
            fileManager = FileManager(this)
        )
        currentComponent = MutableStateFlow(
            SplashScreen(
                repoStore = repoStore,
                showContent = ::showContent
            )
        )
        setContent {
            val currentComponent by currentComponent.collectAsState()
            currentComponent.show(
                Modifier.fillMaxSize()
                    .systemBarsPadding()
            )
        }
    }

    private fun showContent() {
        currentComponent.update {
            it.clear()
            Main(
                repoStore = repoStore,
                mediaPlayer = MediaPlayer(this),
                mediaUtil = MediaUtil(this),
                this
            )
        }
    }
}