package dev.younesgouyd.apps.music.client.multiplatform

import dev.younesgouyd.apps.music.common.MediaPlayer
import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.WSMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.KSerializer
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid

class ServerRequestsHandler(
    private val requests: ReceiveChannel<WSMessage.Request>,
    private val mediaPlayer: MediaPlayer
) {
    private val logger = KotlinLogging.logger {}
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val jobs = ConcurrentHashMap<Uuid, Job>()

    val responses: ReceiveChannel<WSMessage.Response>
        field = Channel<WSMessage.Response>(Channel.UNLIMITED)

    init {
        coroutineScope.launch {
            requests.consumeEach { request ->
                when (request) {
                    is WSMessage.Request.Execute -> {
                        jobs[request.correlationId]?.cancel() // TODO
                        jobs[request.correlationId] = coroutineScope.launch {
                            try {
                                process(
                                    correlationId = request.correlationId,
                                    rpc = request.rpc as? Rpc.ToClient ?: TODO()
                                )
                            } finally {
                                jobs.remove(request.correlationId)
                            }
                        }
                    }
                    is WSMessage.Request.Cancel -> {
                        jobs.remove(request.correlationId)?.cancel()
                    }
                }
            }
        }
    }

    fun release() { // TODO
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        logger.debug { "::release | cancelled all jobs" }
    }

    private suspend fun process(correlationId: Uuid, rpc: Rpc.ToClient) {
        when (rpc) {
            is Rpc.ToClient.MediaPlayer -> {
                when (rpc) {
                    is Rpc.ToClient.MediaPlayer.ObserveState -> {
                        mediaPlayer.state.collect {
                            sendData(correlationId, it, rpc.serializer)
                        }
                    }
                    is Rpc.ToClient.MediaPlayer.SetTrack -> {
                        mediaPlayer.setTrack(rpc.id)
                        sendDone(correlationId)
                    }
                    is Rpc.ToClient.MediaPlayer.Play -> {
                        mediaPlayer.play(rpc.context)
                        sendDone(correlationId)
                    }
                    is Rpc.ToClient.MediaPlayer.Pause -> {
                        mediaPlayer.pause()
                        sendDone(correlationId)
                    }
                    is Rpc.ToClient.MediaPlayer.Stop -> {
                        mediaPlayer.stop()
                        sendDone(correlationId)
                    }
                    is Rpc.ToClient.MediaPlayer.SetTime -> {
                        mediaPlayer.setPositionMs(rpc.positionMs, rpc.context)
                        sendDone(correlationId)
                    }
                    is Rpc.ToClient.MediaPlayer.SetState -> {
                        mediaPlayer.setState(rpc.value, rpc.context)
                        sendDone(correlationId)
                    }
                }
            }
        }
    }

    private suspend fun <T> sendData(correlationId: Uuid, data: T, serializer: KSerializer<T>) {
        responses.send(
            WSMessage.Response.Data(
                correlationId = correlationId,
                value = json.encodeToJsonElement(serializer, data)
            )
        )
    }

    private suspend fun sendDone(correlationId: Uuid) {
        responses.send(
            WSMessage.Response.Done(correlationId)
        )
    }
}