package dev.younesgouyd.apps.music.client.multiplatform

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.ResultType
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.WSMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class ClientRequestsHandler(
    tempDir: File,
    private val responses: ReceiveChannel<WSMessage.Response>,
    downloadTo: suspend (MediaFileId, File) -> Unit
) : Server() {
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val responseChannels = ConcurrentHashMap<Uuid, SendChannel<WSMessage.Response>>()
    private val states = ConcurrentHashMap<Rpc.ToServer, SharedFlow<Any?>>()
    private val fileManager: FileManager = FileManager(tempDir, coroutineScope, downloadTo)

    val requests: ReceiveChannel<WSMessage.Request>
        field = Channel<WSMessage.Request>(Channel.UNLIMITED)

    init {
        coroutineScope.launch {
            responses.consumeEach { response ->
                responseChannels[response.correlationId]?.trySend(response)
            }
        }
    }

    override suspend fun <TRpc> call(rpc: TRpc) where TRpc: Rpc.ToServer, TRpc: ResultType.Action {
        val correlationId = Uuid.random()
        val channel = Channel<WSMessage.Response>(Channel.CONFLATED)
        responseChannels[correlationId] = channel
        try {
            val request: WSMessage.Request = WSMessage.Request.Execute(correlationId, rpc)
            requests.send(request)
            when (channel.receive()) {
                is WSMessage.Response.Done -> return
                is WSMessage.Response.Data -> { TODO() }
            }
        } catch (e: CancellationException) {
            requests.send(WSMessage.Request.Cancel(correlationId))
            throw e
        } finally {
            responseChannels.remove(correlationId)
            channel.close()
        }
    }

    override suspend fun <TRpc, TResult> get(rpc: TRpc): TResult where TRpc: Rpc.ToServer, TRpc: ResultType.Data.Static<TResult> {
        val correlationId = Uuid.random()
        val channel = Channel<WSMessage.Response>(Channel.CONFLATED)
        responseChannels[correlationId] = channel
        try {
            val request: WSMessage.Request = WSMessage.Request.Execute(correlationId, rpc)
            requests.send(request)
            return when (val response = channel.receive()) {
                is WSMessage.Response.Done -> TODO()
                is WSMessage.Response.Data -> { json.decodeFromJsonElement(rpc.serializer, response.value) }
            }
        } catch (e: CancellationException) {
            requests.send(WSMessage.Request.Cancel(correlationId))
            throw e
        } finally {
            responseChannels.remove(correlationId)
            channel.close()
        }
    }

    override fun <TRpc, TResult> observe(rpc: TRpc): Flow<TResult> where TRpc: Rpc.ToServer, TRpc: ResultType.Data.Dynamic<TResult> {
        return flow {
            val activeStream = states.computeIfAbsent(rpc) { key ->
                lateinit var sharedFlow: SharedFlow<TResult>
                sharedFlow = flow {
                    val correlationId = Uuid.random()
                    val channel = Channel<WSMessage.Response>(Channel.CONFLATED)
                    responseChannels[correlationId] = channel
                    try {
                        val request: WSMessage.Request = WSMessage.Request.Execute(correlationId, rpc)
                        requests.send(request)
                        for (state in channel) {
                            val parsed = when (state) {
                                is WSMessage.Response.Done -> TODO()
                                is WSMessage.Response.Data -> json.decodeFromJsonElement(rpc.serializer, state.value)
                            }
                            emit(parsed)
                        }
                    } finally {
                        responseChannels.remove(correlationId)
                        channel.close()
                        requests.send(WSMessage.Request.Cancel(correlationId))
                    }
                }.distinctUntilChanged()
                    .onCompletion { states.remove(key, sharedFlow) }
                    .shareIn(
                        scope = coroutineScope,
                        started = SharingStarted.WhileSubscribed(5.seconds),
                        replay = 1
                    )
                sharedFlow
            } as SharedFlow<TResult>
            emitAll(activeStream)
        }
    }

    override suspend fun getFile(id: MediaFileId): File {
        return fileManager.get(id)
    }

    fun release() {
        responseChannels.values.forEach { it.close() }
        responseChannels.clear()
        fileManager.release()
    }

    private class FileManager(
        private val tempDir: File,
        private val coroutineScope: CoroutineScope,
        private val downloadTo: suspend (MediaFileId, File) -> Unit
    ) {
        private val logger = KotlinLogging.logger {}
        private val fileCache = ConcurrentHashMap<MediaFileId, Deferred<File>>()

        suspend fun get(id: MediaFileId): File {
            val file = File(tempDir, id.toString())
            if (file.exists()) {
                return file
            }
            val deferred = fileCache.computeIfAbsent(id) { key ->
                coroutineScope.async(Dispatchers.IO) {
                    val tempFile = File(tempDir, "${key.value}_${Uuid.random()}.tmp")
                    try {
                        logger.debug { "--> ::get | downloading file $id" }
                        downloadTo(id, tempFile)
                        tempFile.copyTo(file, overwrite = true)
                        tempFile.delete()
                        file
                    } catch (e: Throwable) {
                        logger.error(e) { "... ::get | failed downloading media file $id" }
                        throw e
                    } finally {
                        if (tempFile.exists()) { tempFile.delete() }
                        if (!file.exists()) { fileCache.remove(key) }
                    }
                }
            }
            return deferred.await()
        }

        fun release() {
            fileCache.clear()
        }
    }
}