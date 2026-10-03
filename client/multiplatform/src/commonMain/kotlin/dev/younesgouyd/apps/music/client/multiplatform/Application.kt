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
import dev.younesgouyd.apps.music.common.getLocalInetAddress
import dev.younesgouyd.apps.music.common.nsdServiceNameToServiceType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.*
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener
import kotlin.coroutines.resume
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

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
            onConnect = { nsdServiceName: String, serverFingerprint: String, password: String ->
                coroutineScope.launch {
                    try {
                        loading.value = true
                        val result = start(nsdServiceName, serverFingerprint, password)
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
    private val nsdServiceNameFile = File(serverInfoDir, "nsdservicename.txt")
    private val fingerprintFile = File(serverInfoDir, "fingerprint.txt")
    private val passwordFile = File(serverInfoDir, "password.txt")

    init {
        coroutineScope.launch {
            loading.value = true
            var nsdServiceName = ""
            var fng = ""
            var pass = ""
            withContext(Dispatchers.IO) {
                appDir.mkdir()
                tempDir.mkdir()
                if (tempMediaFilesDir.exists()) { tempMediaFilesDir.deleteRecursively() }
                tempMediaFilesDir.mkdir()
                serverInfoDir.mkdir()
                if (!nsdServiceNameFile.exists()) nsdServiceNameFile.createNewFile()
                if (!fingerprintFile.exists()) fingerprintFile.createNewFile()
                if (!passwordFile.exists()) passwordFile.createNewFile()
                nsdServiceName = nsdServiceNameFile.readText()
                fng = fingerprintFile.readText()
                pass = passwordFile.readText()
            }
            if (nsdServiceName.isNotBlank() && fng.isNotBlank() && pass.isNotBlank()) {
                try {
                    val result = start(nsdServiceName, fng, pass)
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
        nsdServiceName: String,
        serverFingerprint: String,
        password: String
    ): Result<Unit> {
        val serverIpAddress = try {
            getServerIpAddress(nsdServiceName)
        } catch (e: Throwable) {
            logger.error(e) {}
            return Result.failure(e)
        }
        val disconnection = CompletableDeferred<Unit>()
        val srvr = Server(serverIpAddress, serverFingerprint, password, tempMediaFilesDir, disconnection)
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
                nsdServiceNameFile.writeText(nsdServiceName)
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

    private suspend fun getServerIpAddress(nsdServiceName: String): String {
        return withContext(Dispatchers.IO) {
            withTimeout(5.seconds) {
                val jmDns = JmDNS.create(
                    getLocalInetAddress() ?: throw IllegalStateException("Could not determine local InetAddress")
                )
                val serviceType = nsdServiceNameToServiceType(nsdServiceName)
                suspendCancellableCoroutine { cont ->
                    val listener = object : ServiceListener {
                        override fun serviceAdded(event: ServiceEvent) {
                            if (event.name == nsdServiceName) {
                                jmDns.requestServiceInfo(event.type, event.name)
                            }
                        }
                        override fun serviceResolved(event: ServiceEvent) {
                            if (event.name == nsdServiceName) {
                                val ipAddress = event.info.inet4Addresses.firstOrNull()?.hostAddress
                                    ?: event.info.inetAddresses.firstOrNull { !it.isLinkLocalAddress }?.hostAddress
                                    ?: event.info.hostAddresses.firstOrNull()
                                if (ipAddress != null && cont.isActive) {
                                    try {
                                        jmDns.removeServiceListener(serviceType, this)
                                    } catch (_: Exception) {}
                                    cont.resume(ipAddress)
                                }
                            }
                        }
                        override fun serviceRemoved(event: ServiceEvent) {}
                    }
                    jmDns.addServiceListener(serviceType, listener)
                    cont.invokeOnCancellation {
                        try {
                            jmDns.removeServiceListener(serviceType, listener)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }
}
