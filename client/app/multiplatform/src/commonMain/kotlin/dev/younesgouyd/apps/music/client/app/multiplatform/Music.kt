package dev.younesgouyd.apps.music.client.app.multiplatform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.client.app.multiplatform.components.Main
import dev.younesgouyd.apps.music.client.app.multiplatform.components.ReinitializeAppData
import dev.younesgouyd.apps.music.client.app.multiplatform.components.SplashScreen
import dev.younesgouyd.apps.music.client.app.multiplatform.data.RepoStore
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.AppDatabase
import dev.younesgouyd.apps.music.client.app.multiplatform.util.Component
import dev.younesgouyd.apps.music.client.app.multiplatform.util.copyTo
import dev.younesgouyd.apps.music.client.app.multiplatform.util.use2
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

abstract class Music {
    companion object {
        private val logger = KotlinLogging.logger {}
        var coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
            private set

        private var backHandlers: Stack<() -> Unit> = Stack()

        fun registerBackHandler(onBack: () -> Unit) {
            backHandlers.push(onBack)
        }

        fun unregisterLastBackHandler() {
            try {
                backHandlers.pop()
            } catch (e: EmptyStackException) {
                logger.warn(e) { }
            }
        }
    }
    private val logger = KotlinLogging.logger {}

    protected abstract val appDir: File
    protected abstract val dbDir: File
    protected abstract val dbFile: File

    protected lateinit var db: AppDatabase
    protected lateinit var repoStore: RepoStore
    private val currentComponent: MutableStateFlow<Component>
    protected lateinit var mediaPlayer: MediaController.MediaPlayer

    init {
        currentComponent = MutableStateFlow(SplashScreen())
        coroutineScope.launch {
            initApp()
            showContent()
        }
    }

    protected abstract suspend fun initDb()
    protected abstract suspend fun createMediaPlayer()
    protected abstract fun openZipInputStreamFromUri(uri: String): ZipInputStream

    @Composable
    fun show(modifier: Modifier) {
        val currentComponent by currentComponent.collectAsState()

        currentComponent.show(modifier.fillMaxSize())
    }

    fun clear() {
        currentComponent.value.clear()
        coroutineScope.cancel()
    }

    fun navigateBack() {
        backHandlers.lastOrNull()?.invoke()
    }

    protected suspend fun initApp() {
        logger.info { "--> initApp" }
        System.setProperty("sun.java2d.uiScale", "1.0")
        withContext(Dispatchers.IO) {
            if (!appDir.exists()) {
                logger.info { "... initApp | creating appDir" }
                appDir.mkdir()
            }
        }
        initDb()
        createMediaPlayer()
        this.repoStore = RepoStore(
            appDir = appDir,
            dbDir = dbDir,
            applicationScope = coroutineScope,
            database = db,
            mediaPlayer = mediaPlayer
        ).also { it.init() }
        logger.info { "<-- initApp" }
    }

    protected fun reinitApp(sourceFileUri: String) {
        logger.info { "--> ::reinitApp" }
        currentComponent.value.clear()
        mediaPlayer.release()
        coroutineScope.cancel()
        db.close()
        coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                dbDir.deleteRecursively()
                repoStore.fileManager.inspectionDir.deleteRecursively()
                repoStore.fileManager.mediaDir.deleteRecursively()
                dbDir.mkdir()
                repoStore.fileManager.inspectionDir.mkdir()
                repoStore.fileManager.mediaDir.mkdir()
            }
            openZipInputStreamFromUri(sourceFileUri).use2 {
                populateAppData(it)
            }
            initApp()
            showContent()
        }
    }

    private fun File.deleteRecursively() {
        for (file in this.listFiles().orEmpty()) {
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete().also { if (!it) TODO() }
            }
        }
        if (this.isDirectory) {
            this.delete()
        }
    }

    private fun showContent() {
        currentComponent.update {
            it.clear()
            Main(
                repoStore = repoStore,
                onReinitializeAppData = ::showReInitializeAppData
            )
        }
    }

    private fun showReInitializeAppData() {
        currentComponent.update {
            it.clear()
            ReinitializeAppData(::reinitApp)
        }
    }

    private suspend fun populateAppData(zip: ZipInputStream) {
        logger.info { "--> ::populateAppData" }
        withContext(Dispatchers.IO) {
            val inspectionDir = repoStore.fileManager.inspectionDir
            val mediaDir = repoStore.fileManager.mediaDir
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                entry.name.let {
                    logger.info { "... ::populateAppData | entry.name: $it" }
                }

                if (entry.isDirectory) {
                    zip.closeEntry()
                    entry = zip.nextEntry
                    continue
                }
                if (entry.name.startsWith("db/")) {
                    zip.copyTo(
                        File(dbDir, entry.name.substringAfterLast("/"))
                    )
                } else if (entry.name.startsWith("inspections/")) {
                    zip.copyTo(
                        File(inspectionDir, entry.name.substringAfterLast("/"))
                    )
                } else if (entry.name.startsWith("media/")) {
                    zip.copyTo(
                        File(mediaDir, entry.name.substringAfterLast("/"))
                    )
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

}

expect class MusicImpl : Music
