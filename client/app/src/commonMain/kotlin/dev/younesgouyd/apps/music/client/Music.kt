package dev.younesgouyd.apps.music.client

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.client.components.Main
import dev.younesgouyd.apps.music.client.components.ReinitializeAppData
import dev.younesgouyd.apps.music.client.components.SplashScreen
import dev.younesgouyd.apps.music.client.data.RepoStore
import dev.younesgouyd.apps.music.client.data.room.AppDatabase
import dev.younesgouyd.apps.music.client.util.Component
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

abstract class Music {
    companion object {
        var coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
            private set
    }

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

    protected suspend fun initApp() {
        println("--> initApp")
        System.setProperty("sun.java2d.uiScale", "1.0")
        withContext(Dispatchers.IO) {
            if (!appDir.exists()) {
                println("... initApp | creating appDir")
                appDir.mkdir()
            }
        }
        initDb()
        this.repoStore = RepoStore(
            appDir = appDir,
            dbDir = dbDir,
            applicationScope = coroutineScope,
            database = db
        ).also { it.init() }
        createMediaPlayer()
    }

    protected fun reinitApp(sourceFileUri: String) {
        println("--> ::reinitApp")
        currentComponent.value.clear()
        mediaPlayer.release()
        coroutineScope.cancel()
        db.close()
        coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                dbDir.deleteRecursively().also { if (!it) TODO() }
                repoStore.fileManager.inspectionDir.deleteRecursively().also { if (!it) TODO() }
                repoStore.fileManager.mediaDir.deleteRecursively().also { if (!it) TODO() }
                dbDir.mkdir()
                repoStore.fileManager.inspectionDir.mkdir()
                repoStore.fileManager.mediaDir.mkdir()
            }
            openZipInputStreamFromUri(sourceFileUri).use {
                populateAppData(it)
            }
            initApp()
            showContent()
        }
    }

    private fun showContent() {
        currentComponent.update {
            it.clear()
            Main(
                repoStore = repoStore,
                mediaPlayer = mediaPlayer,
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
        println("--> ::populateAppData")
        withContext(Dispatchers.IO) {
            val inspectionDir = repoStore.fileManager.inspectionDir
            val mediaDir = repoStore.fileManager.mediaDir
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                println("... ::populateAppData | entry.name: ${entry.name}")
                if (entry.isDirectory) {
                    zip.closeEntry()
                    entry = zip.nextEntry
                    continue
                }
                if (entry.name.startsWith("db/")) {
                    zip.copyTo(
                        File(dbDir, entry.name.substringAfterLast("/")).outputStream()
                    )
                } else if (entry.name.startsWith("inspections/")) {
                    zip.copyTo(
                        File(inspectionDir, entry.name.substringAfterLast("/")).outputStream()
                    )
                } else if (entry.name.startsWith("media/")) {
                    zip.copyTo(
                        File(mediaDir, entry.name.substringAfterLast("/")).outputStream()
                    )
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
}

expect class MusicImpl : Music