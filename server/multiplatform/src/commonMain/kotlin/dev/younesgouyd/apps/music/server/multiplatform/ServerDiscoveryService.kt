package dev.younesgouyd.apps.music.server.multiplatform

import dev.younesgouyd.apps.music.common.ServerDiscoveryConstants
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException

class ServerDiscoveryService {
    private val logger = KotlinLogging.logger {}
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _status: MutableStateFlow<Status> = MutableStateFlow(Status.Stopped)
    @Volatile private var socket: DatagramSocket? = null

    val status: StateFlow<Status> = _status.asStateFlow()

    fun start() {
        if (!_status.compareAndSet(expect = Status.Stopped, update = Status.Running)) {
            logger.warn { "::start | already running" }
            return
        }
        coroutineScope.launch {
            var localSocket: DatagramSocket? = null
            try {
                localSocket = DatagramSocket(ServerDiscoveryConstants.DISCOVERY_PORT)
                socket = localSocket
                val requestMagic = ServerDiscoveryConstants.REQUEST_MAGIC.toByteArray(Charsets.UTF_8)
                val responseMagic = ServerDiscoveryConstants.RESPONSE_MAGIC.toByteArray(Charsets.UTF_8)
                while (_status.value == Status.Running) {
                    val packet = run {
                        val buffer = ByteArray(requestMagic.size)
                        DatagramPacket(buffer, buffer.size)
                    }
                    localSocket.receive(packet)
                    if (packet.data.contentEquals(requestMagic)) {
                        localSocket.send(
                            DatagramPacket(
                                responseMagic,
                                responseMagic.size,
                                packet.address,
                                packet.port
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is SocketException && _status.value == Status.Stopped) {
                    logger.debug { "::start | UDP discovery socket closed normally" }
                } else {
                    logger.error(e) { "::start | UDP discovery error" }
                }
            } finally {
                _status.value = Status.Stopped
                try {
                    localSocket?.close()
                } catch (e: Exception) {
                    logger.error(e) {}
                }
                socket = null
            }
        }
    }

    fun stop() {
        if (_status.compareAndSet(expect = Status.Running, update = Status.Stopped)) {
            try {
                socket?.close()
            } catch (e: Exception) {
                logger.error(e) {}
            }
            socket = null
        }
    }

    enum class Status { Stopped, Running }
}