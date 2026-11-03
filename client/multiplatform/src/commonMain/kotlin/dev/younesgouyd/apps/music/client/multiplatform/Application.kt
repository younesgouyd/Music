package dev.younesgouyd.apps.music.client.multiplatform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.client.multiplatform.components.Main
import dev.younesgouyd.apps.music.client.multiplatform.components.StartupScreen
import dev.younesgouyd.apps.music.common.ServerDiscoveryConstants
import dev.younesgouyd.apps.music.common.models.datamodels.PlaybackInterval
import dev.younesgouyd.apps.music.common.models.datamodels.PlaybackIntervalStart
import dev.younesgouyd.apps.music.common.ui.Component
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.*
import kotlin.system.exitProcess

abstract class Application(appDir: File) {
    companion object {
        protected const val APP_DIR_NAME = "younesmusicdata"

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

    private val logger = KotlinLogging.logger {}
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val loading = MutableStateFlow(false)
    private val error: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val currentComponent: MutableStateFlow<Component> = MutableStateFlow(
        StartupScreen(
            onConnect = { serverFingerprint: String, password: String ->
                coroutineScope.launch {
                    loading.value = true
                    start(serverFingerprint, password)
                    loading.value = false
                }
            },
            loading = loading.asStateFlow(),
            error = error.asStateFlow()
        )
    )
    private val tempDir = File(appDir, "temp")
    private val tempMediaFilesDir = File(tempDir, "media")
    private val serverInfoDir = File(appDir, "server_info")
    private val fingerprintFile = File(serverInfoDir, "fingerprint.txt")
    private val passwordFile = File(serverInfoDir, "password.txt")
    private val clientIdFile = File(serverInfoDir, "client_id.txt")
    private val playbackIntervalsDir = File(appDir, "playback_intervals")

    init {
        coroutineScope.launch {
            loading.value = true
            var fng = ""
            var pass = ""
            withContext(Dispatchers.IO) {
                appDir.mkdir()
                tempDir.mkdir()
                if (tempMediaFilesDir.exists()) { tempMediaFilesDir.deleteRecursively() }
                tempMediaFilesDir.mkdir()
                serverInfoDir.mkdir()
                if (!fingerprintFile.exists()) fingerprintFile.createNewFile()
                if (!passwordFile.exists()) passwordFile.createNewFile()
                if (!clientIdFile.exists()) clientIdFile.createNewFile()
                playbackIntervalsDir.mkdir()
                fng = fingerprintFile.readText()
                pass = passwordFile.readText()
            }
            if (fng.isNotBlank() && pass.isNotBlank()) {
                start(fng, pass)
            }
            loading.value = false
        }
    }

    private suspend fun start(
        serverFingerprint: String,
        password: String
    ) {
        val serverIpAddress = try {
            getServerIpAddress()!!
        } catch (e: Throwable) {
            logger.error(e) {}
            error.value = true
            return
        }
        val connection = ServerConnection(
            serverHost = serverIpAddress,
            serverFingerprint = serverFingerprint,
            password = password,
            clientIdFile = clientIdFile
        )
        try {
            logger.debug { "connecting..." }
            connection.connect()
        } catch (e: Throwable) {
            logger.error(e) {}
            connection.close()
            error.value = true
            return
        }
        error.value = false
        logger.debug { "init ClientRequestsHandler..." }
        val clientRequestsHandler = ClientRequestsHandler(
            tempDir = tempDir,
            responses = connection.responses,
            downloadTo = connection::downloadTo
        )
        val playbackIntervals = Channel<PlaybackInterval>(Channel.UNLIMITED)
        val playbackIntervalsStarts = Channel<PlaybackIntervalStart>(Channel.UNLIMITED)
        logger.debug { "init LocalMediaPlayer..." }
        val mediaPlayer = createMediaPlayer(
            clientId = connection.clientId.await(),
            playbackIntervals = playbackIntervals,
            playbackIntervalsStarts = playbackIntervalsStarts,
            getFile = clientRequestsHandler::getFile
        )
        logger.debug { "init ServerRequestsHandler..." }
        val serverRequestsHandler = ServerRequestsHandler(
            requests = connection.requests,
            mediaPlayer = mediaPlayer
        )
        logger.debug { "init PlaybackIntervalDispatcher..." }
        val playbackIntervalDispatcher = PlaybackIntervalDispatcher(
            server = clientRequestsHandler,
            source = playbackIntervals,
            starts = playbackIntervalsStarts,
            playbackIntervalsDir = playbackIntervalsDir
        )
        coroutineScope.launch {
            connection.disconnection.await()
            logger.debug { "disconnected!" }
            clientRequestsHandler.release()
            serverRequestsHandler.release()
            mediaPlayer.release()
            playbackIntervalDispatcher.stopSending()
            playbackIntervalDispatcher.waitRemaining()
            exitProcess(1) // TODO
        }
        coroutineScope.launch {
            serverRequestsHandler.responses.consumeEach {
                connection.send(it)
            }
        }
        coroutineScope.launch {
            clientRequestsHandler.requests.consumeEach {
                connection.send(it)
            }
        }
        playbackIntervalDispatcher.start()
        withContext(Dispatchers.IO) {
            fingerprintFile.writeText(serverFingerprint)
            passwordFile.writeText(password)
        }
        dev.younesgouyd.apps.music.client.multiplatform.components.util.server = clientRequestsHandler // TODO
        currentComponent.update {
            it.clear()
            Main(clientRequestsHandler)
        }
    }

    @Composable
    fun show(modifier: Modifier) {
        val currentComponent by currentComponent.collectAsState()

        currentComponent.show(modifier.fillMaxSize())
    }

    fun navigateBack() {
        backHandlers.lastOrNull()?.invoke()
    }

    private suspend fun getServerIpAddress(): String? {
        val attempts = 5
        return withContext(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket().apply {
                    broadcast = true
                    soTimeout = 500
                }
                val requestMagic = ServerDiscoveryConstants.REQUEST_MAGIC.toByteArray(Charsets.UTF_8)
                val responseMagic = ServerDiscoveryConstants.RESPONSE_MAGIC.toByteArray(Charsets.UTF_8)
                val requestPacket = DatagramPacket(
                    requestMagic,
                    requestMagic.size,
                    InetAddress.getByName("255.255.255.255"),
                    ServerDiscoveryConstants.DISCOVERY_PORT
                )
                repeat(attempts) { attempt ->
                    try {
                        socket.send(requestPacket)
                        val packet = run {
                            val buffer = ByteArray(responseMagic.size)
                            DatagramPacket(buffer, buffer.size)
                        }
                        socket.receive(packet)
                        if (packet.data.contentEquals(responseMagic)) {
                            logger.debug { "::getServerIpAddress | attempt ${attempt + 1}/$attempts: received valid payload from ${packet.address.hostAddress}:${packet.port}" }
                            return@withContext packet.address.hostAddress
                        } else {
                            logger.error { "::getServerIpAddress | attempt ${attempt + 1}/$attempts: received invalid payload from ${packet.address.hostAddress}:${packet.port}" }
                        }
                    } catch (_: SocketTimeoutException) {
                        logger.debug { "::getServerIpAddress | attempt ${attempt + 1}/$attempts: timed out" }
                    }
                }
                logger.info { "::getServerIpAddress | server not found after $attempts attempts" }
                null
            } catch (e: Exception) {
                logger.error(e) {}
                null
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }
    }
}
