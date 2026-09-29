package dev.younesgouyd.apps.music.client.multiplatform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.client.multiplatform.components.Main
import dev.younesgouyd.apps.music.client.multiplatform.components.StartupScreen
import dev.younesgouyd.apps.music.client.multiplatform.data.RepoStore
import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.client.multiplatform.usecases.*
import dev.younesgouyd.apps.music.client.multiplatform.util.Component
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.*
import kotlin.system.exitProcess

class Application {
    companion object {
        private val logger = KotlinLogging.logger {}

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

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var mediaController: MediaController
    private lateinit var server: Server
    private lateinit var repoStore: RepoStore

    private lateinit var unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase
    private lateinit var setTrackMetadataFromSpotifyUseCase: SetTrackMetadataFromSpotifyUseCase
    private lateinit var clearImportItemUseCase: ClearImportItemUseCase
    private lateinit var deleteFolderUseCase: DeleteFolderUseCase
    private lateinit var prepareImportUseCase: PrepareImportUseCase

    private val loading = MutableStateFlow(false)
    private val error: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val currentComponent: MutableStateFlow<Component> = MutableStateFlow(
        StartupScreen(
            onConnect = { serverIpAddr: String, serverFingerprint: String, password: String ->
                coroutineScope.launch {
                    try {
                        loading.value = true
                        val result = start(serverIpAddr, serverFingerprint, password)
                        val err = result.exceptionOrNull()
                        if (err != null) {
                            logger.error(err) {}
                            error.value = true
                        } else {
                            error.value = false
                        }
                        loading.value = false
                    } catch (e: Throwable) {
                        logger.error(e) {}
                    }
                }
            },
            loading = loading.asStateFlow(),
            error = error.asStateFlow()
        )
    )
    private val tempDir = File(appDir, "temp")
    private val tempMediaFilesDir = File(tempDir, "media")
    private val serverInfoDir = File(appDir, "server_info")
    private val ipAddressFile = File(serverInfoDir, "ip_address.txt")
    private val fingerprintFile = File(serverInfoDir, "fingerprint.txt")
    private val passwordFile = File(serverInfoDir, "password.txt")

    init {
        coroutineScope.launch {
            loading.value = true
            var ipAddr = ""
            var fng = ""
            var pass = ""
            withContext(Dispatchers.IO) {
                appDir.mkdir()
                tempDir.mkdir()
                if (tempMediaFilesDir.exists()) { tempMediaFilesDir.deleteRecursively() }
                tempMediaFilesDir.mkdir()
                serverInfoDir.mkdir()
                if (!ipAddressFile.exists()) ipAddressFile.createNewFile()
                if (!fingerprintFile.exists()) fingerprintFile.createNewFile()
                if (!passwordFile.exists()) passwordFile.createNewFile()
                ipAddr = ipAddressFile.readText()
                fng = fingerprintFile.readText()
                pass = passwordFile.readText()
            }
            if (ipAddr.isNotBlank() && fng.isNotBlank() && pass.isNotBlank()) {
                try {
                    val result = start(ipAddr, fng, pass)
                    val err = result.exceptionOrNull()
                    if (err != null) {
                        logger.error(err) {}
                        error.value = true
                    } else {
                        error.value = false
                    }
                } catch (e: Throwable) {
                    logger.error(e) {}
                }
            }
            loading.value = false
        }
    }

    private suspend fun start(
        serverIpAddr: String,
        serverFingerprint: String,
        password: String
    ): Result<Unit> {
        val disconnection = CompletableDeferred<Unit>()
        val srvr = Server(serverIpAddr, serverFingerprint, password, tempMediaFilesDir, disconnection)
        val disconnectionJob = coroutineScope.launch {
            disconnection.await()
            logger.debug { "disconnected!" }
            clear()
            exitProcess(1) // TODO
        }
        val rslt: Result<Unit> = runCatching { srvr.connect() }
        if (rslt.isFailure) {
            srvr.close()
            disconnectionJob.cancel()
            return rslt
        } else {
            withContext(Dispatchers.IO) {
                logger.debug { "writing server info" }
                ipAddressFile.writeText(serverIpAddr)
                fingerprintFile.writeText(serverFingerprint)
                passwordFile.writeText(password)
            }
            server = srvr
            dev.younesgouyd.apps.music.client.multiplatform.components.util.server = server // TODO
            repoStore = RepoStore(server)
            unsetSpotifyTrackUseCase = UnsetSpotifyTrackUseCase(server)
            setTrackMetadataFromSpotifyUseCase = SetTrackMetadataFromSpotifyUseCase(server)
            clearImportItemUseCase = ClearImportItemUseCase(server)
            deleteFolderUseCase = DeleteFolderUseCase(server)
            prepareImportUseCase = PrepareImportUseCase(server)
            mediaController = MediaController(server)
            logger.debug { "updating to main component" }
            currentComponent.update {
                it.clear()
                Main(
                    repoStore = repoStore,
                    deleteFolderUseCase = deleteFolderUseCase,
                    clearImportItemUseCase = clearImportItemUseCase,
                    setTrackMetadataFromSpotifyUseCase = setTrackMetadataFromSpotifyUseCase,
                    unsetSpotifyTrackUseCase = unsetSpotifyTrackUseCase,
                    prepareImportUseCase = prepareImportUseCase,
                    mediaController = mediaController,
                )
            }
            return Result.success(Unit)
        }
    }

    @Composable
    fun show(modifier: Modifier) {
        val currentComponent by currentComponent.collectAsState()

        currentComponent.show(modifier.fillMaxSize())
    }

    fun clear() {
        try { currentComponent.value.clear() } catch (e: Throwable) { logger.error(e) {} }
        try { mediaController.release() } catch (e: Throwable) { logger.error(e) {} }
        try { runBlocking { server.close() } } catch (e: Throwable) { logger.error(e) {} }
        try { coroutineScope.cancel() } catch (e: Throwable) { logger.error(e) {} }
    }

    fun navigateBack() {
        backHandlers.lastOrNull()?.invoke()
    }
}
