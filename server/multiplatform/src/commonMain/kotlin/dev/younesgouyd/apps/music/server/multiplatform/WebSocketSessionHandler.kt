package dev.younesgouyd.apps.music.server.multiplatform

import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.ResultType
import dev.younesgouyd.apps.music.common.models.Rpc
import dev.younesgouyd.apps.music.common.models.WSMessage
import dev.younesgouyd.apps.music.server.multiplatform.data.RepoStore
import dev.younesgouyd.apps.music.server.multiplatform.data.toModel
import dev.younesgouyd.apps.music.server.multiplatform.spotify.Spotify
import dev.younesgouyd.apps.music.server.multiplatform.usecases.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.websocket.*
import io.ktor.utils.io.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class WebSocketSessionHandler(
    private val clientId: ClientId,
    private val session: DefaultWebSocketServerSession,
    repoStore: RepoStore,
    mediaController: MediaController,
    ytDlp: YtDlp,
    spotify: Spotify,
    unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase,
    setTrackMetadataFromSpotifyUseCase: SetTrackMetadataFromSpotifyUseCase,
    tagItemsUseCase: TagItemsUseCase,
    addToPlaylistUseCase: AddToPlaylistUseCase,
    deleteUseCase: DeleteUseCase,
    prepareImportUseCase: PrepareImportUseCase,
    moveItemsToFolderUseCase: MoveItemsToFolderUseCase,
    playbackIntervalManager: PlaybackIntervalManager
) {
    private val logger = KotlinLogging.logger {}
    private val serverRequestsHandler: ServerRequestsHandler = ServerRequestsHandler(session)
    private val clientRequestsHandler: ClientRequestsHandler = ClientRequestsHandler(
        clientId = clientId,
        coroutineScope = session,
        repoStore = repoStore,
        mediaController = mediaController,
        ytDlp = ytDlp,
        spotify = spotify,
        unsetSpotifyTrackUseCase = unsetSpotifyTrackUseCase,
        setTrackMetadataFromSpotifyUseCase = setTrackMetadataFromSpotifyUseCase,
        tagItemsUseCase = tagItemsUseCase,
        addToPlaylistUseCase = addToPlaylistUseCase,
        deleteUseCase = deleteUseCase,
        prepareImportUseCase = prepareImportUseCase,
        moveItemsToFolderUseCase = moveItemsToFolderUseCase,
        playbackIntervalManager = playbackIntervalManager
    )

    suspend fun run() {
        with(session) {
            launch {
                clientRequestsHandler.responses.consumeEach { message ->
                    logMessage(message, "==>")
                    session.sendSerialized<WSMessage>(message)
                }
            }
            launch {
                serverRequestsHandler.requests.consumeEach { message ->
                    logMessage(message, "==>")
                    session.sendSerialized<WSMessage>(message)
                }
            }
            try {
                while (isActive) {
                    val message = receiveDeserialized<WSMessage>()
                    logMessage(message, "<==")
                    when (message) {
                        is WSMessage.Registration -> { TODO() }
                        is WSMessage.Request -> { clientRequestsHandler.handle(message) }
                        is WSMessage.Response -> { serverRequestsHandler.handle(message) }
                    }
                }
            } finally {
                clientRequestsHandler.release()
                serverRequestsHandler.release()
            }
        }
    }

    suspend fun <TRpc> call(rpc: TRpc) where TRpc: Rpc.ToClient, TRpc: ResultType.Action {
        serverRequestsHandler.call(rpc)
    }

    fun <TRpc, TResult> observe(rpc: TRpc): Flow<TResult> where TRpc: Rpc.ToClient, TRpc: ResultType.Data.Dynamic<TResult> {
        return serverRequestsHandler.observe(rpc)
    }

    fun <TRpc, TResult> stream(rpc: TRpc): Flow<TResult> where TRpc: Rpc.ToClient, TRpc: ResultType.Data.Stream<TResult> {
        return serverRequestsHandler.stream(rpc)
    }

    suspend fun close() {
        session.close()
    }

    private fun logMessage(message: WSMessage, direction: String) {
        val logMessage: String = when (message) {
            is WSMessage.Registration -> {
                "Registration ClientId: ${message.value}"
            }
            is WSMessage.Request -> {
                when (message) {
                    is WSMessage.Request.Execute -> "Request.Execute ${message.correlationId}: ${json.encodeToString(message.rpc)}"
                    is WSMessage.Request.Cancel -> "Request.Cancel ${message.correlationId}"
                }
            }
            is WSMessage.Response -> {
                when (message) {
                    is WSMessage.Response.Data -> "Response.Data ${message.correlationId}: ${json.encodeToString(message.value)}"
                    is WSMessage.Response.Done -> "Response.Done ${message.correlationId}"
                }
            }
        }
        logger.debug { "Client $clientId | $direction $logMessage" }
    }

    private class ServerRequestsHandler(
        private val coroutineScope: CoroutineScope,
    ) {
        private val responseChannels = ConcurrentHashMap<Uuid, SendChannel<WSMessage.Response>>()
        private val states = ConcurrentHashMap<Rpc.ToClient, SharedFlow<Any?>>()

        val requests: ReceiveChannel<WSMessage.Request>
            field = Channel<WSMessage.Request>(Channel.UNLIMITED)

        fun handle(response: WSMessage.Response) {
            responseChannels[response.correlationId]?.trySend(response)
        }

        suspend fun <TRpc> call(rpc: TRpc) where TRpc: Rpc.ToClient, TRpc: ResultType.Action {
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

        fun <TRpc, TResult> observe(rpc: TRpc): Flow<TResult> where TRpc: Rpc.ToClient, TRpc: ResultType.Data.Dynamic<TResult> {
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

        fun <TRpc, TResult> stream(rpc: TRpc): Flow<TResult> where TRpc: Rpc.ToClient, TRpc: ResultType.Data.Stream<TResult> {
            return flow {
                val correlationId = Uuid.random()
                val channel = Channel<WSMessage.Response>(Channel.UNLIMITED)
                responseChannels[correlationId] = channel
                try {
                    val request: WSMessage.Request = WSMessage.Request.Execute(correlationId, rpc)
                    requests.send(request)
                    channel.consumeEach { item ->
                        val parsed = when (item) {
                            is WSMessage.Response.Done -> TODO()
                            is WSMessage.Response.Data -> json.decodeFromJsonElement(rpc.serializer, item.value)
                        }
                        emit(parsed)
                    }
                } finally {
                    responseChannels.remove(correlationId)
                    channel.close()
                    requests.send(WSMessage.Request.Cancel(correlationId))
                }
            }
        }

        fun release() {
            responseChannels.values.forEach { it.close() }
            responseChannels.clear()
        }
    }

    private class ClientRequestsHandler(
        private val clientId: ClientId,
        private val coroutineScope: CoroutineScope,
        private val repoStore: RepoStore,
        private val mediaController: MediaController,
        private val ytDlp: YtDlp,
        private val spotify: Spotify,
        private val unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase,
        private val setTrackMetadataFromSpotifyUseCase: SetTrackMetadataFromSpotifyUseCase,
        private val tagItemsUseCase: TagItemsUseCase,
        private val addToPlaylistUseCase: AddToPlaylistUseCase,
        private val deleteUseCase: DeleteUseCase,
        private val prepareImportUseCase: PrepareImportUseCase,
        private val moveItemsToFolderUseCase: MoveItemsToFolderUseCase,
        private val playbackIntervalManager: PlaybackIntervalManager
    ) {
        private val jobs = ConcurrentHashMap<Uuid, Job>()

        private val clientSettingRepo get() = repoStore.clientSettingRepo
        private val folderRepo get() = repoStore.folderRepo
        private val homeRepo get() = repoStore.homeRepo
        private val playlistRepo get() = repoStore.playlistRepo
        private val playlistTrackCrossRefRepo get() = repoStore.playlistTrackCrossRefRepo
        private val trackRepo get() = repoStore.trackRepo
        private val importSessionRepo get() = repoStore.importSessionRepo
        private val importSessionItemRepo get() = repoStore.importSessionItemRepo
        private val tagRepo get() = repoStore.tagRepo
        private val spotifyAlbumRepo get() = repoStore.spotifyAlbumRepo
        private val spotifyArtistRepo get() = repoStore.spotifyArtistRepo
        private val spotifyTrackRepo get() = repoStore.spotifyTrackRepo

        val responses: ReceiveChannel<WSMessage.Response>
            field = Channel<WSMessage.Response>(Channel.UNLIMITED)

        fun handle(request: WSMessage.Request) {
            when (request) {
                is WSMessage.Request.Execute -> {
                    jobs[request.correlationId]?.cancel() // TODO
                    jobs[request.correlationId] = coroutineScope.launch {
                        try {
                            process(
                                correlationId = request.correlationId,
                                rpc = request.rpc as? Rpc.ToServer ?: TODO()
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

        fun release() { // TODO
            jobs.values.forEach { it.cancel() }
            jobs.clear()
        }

        private suspend fun process(correlationId: Uuid, rpc: Rpc.ToServer) {
            when (rpc) {
                is Rpc.ToServer.UseCase -> when (rpc) {
                    is Rpc.ToServer.UseCase.AddToPlaylist -> {
                        addToPlaylistUseCase.execute(
                            tracks = rpc.tracks,
                            playlists = rpc.playlists,
                            folders = rpc.folders,
                            artists = rpc.artists,
                            albums = rpc.albums,
                            playlist = rpc.playlist
                        )
                        sendDone(correlationId)
                    }
                    is Rpc.ToServer.UseCase.Delete -> {
                        deleteUseCase.execute(
                            tracks = rpc.tracks,
                            playlists = rpc.playlists,
                            folders = rpc.folders
                        )
                        sendDone(correlationId)
                    }
                    is Rpc.ToServer.UseCase.MoveItemsToFolder -> {
                        val result = moveItemsToFolderUseCase.execute(rpc.tracks, rpc.playlists, rpc.folders, rpc.folder)
                        sendData(correlationId, result, rpc.serializer)
                    }
                    is Rpc.ToServer.UseCase.PrepareImport -> {
                        val result = prepareImportUseCase.execute(
                            selected = rpc.selected,
                            url = rpc.url,
                            inspection = rpc.inspection,
                            destinationFolderId = rpc.destinationFolderId
                        )
                        sendData(correlationId, result, rpc.serializer)
                    }
                    is Rpc.ToServer.UseCase.SetTrackMetadataFromSpotify -> {
                        setTrackMetadataFromSpotifyUseCase.execute(
                            trackId = rpc.trackId,
                            spotifyApiTrack = rpc.spotifyApiTrack
                        )
                        sendDone(correlationId)
                    }
                    is Rpc.ToServer.UseCase.TagItems -> {
                        tagItemsUseCase.execute(
                            tracks = rpc.tracks,
                            playlists = rpc.playlists,
                            folders = rpc.folders,
                            artists = rpc.artists,
                            albums = rpc.albums,
                            tags = rpc.tags,
                            unset = rpc.unset
                        )
                    }
                    is Rpc.ToServer.UseCase.UnsetSpotifyTrack -> {
                        unsetSpotifyTrackUseCase.execute(
                            trackId = rpc.trackId,
                            spotifyTrackId = rpc.spotifyTrackId,
                            spotifyAlbumId = rpc.spotifyAlbumId
                        )
                        sendDone(correlationId)
                    }
                }
                is Rpc.ToServer.Inspect -> {
                    val result = ytDlp.inspect(rpc.url).first()
                    sendData(correlationId, result, rpc.serializer)
                }
                is Rpc.ToServer.SetPlaybackIntervalStart -> {
                    playbackIntervalManager.setStart(rpc.start)
                    sendDone(correlationId)
                }
                is Rpc.ToServer.MediaController -> when (rpc) {
                    is Rpc.ToServer.MediaController.ObserveState ->  {
                        mediaController.state.collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.MediaController.ObserveAvailableState -> when (rpc) {
                        is Rpc.ToServer.MediaController.ObserveAvailableState.Clients -> {
                            mediaController.clients.collect { sendData(correlationId, it, rpc.serializer) }
                        }
                        is Rpc.ToServer.MediaController.ObserveAvailableState.Enabled -> {
                            mediaController.enabled.collect { sendData(correlationId, it, rpc.serializer) }
                        }
                        is Rpc.ToServer.MediaController.ObserveAvailableState.Queue -> {
                            mediaController.queue.collect { sendData(correlationId, it, rpc.serializer) }
                        }
                        is Rpc.ToServer.MediaController.ObserveAvailableState.QueueItemIndex -> {
                            mediaController.currentItemIndex.collect { sendData(correlationId, it, rpc.serializer) }
                        }
                        is Rpc.ToServer.MediaController.ObserveAvailableState.RepeatState -> {
                            mediaController.repeatState.collect { sendData(correlationId, it, rpc.serializer) }
                        }
                        is Rpc.ToServer.MediaController.ObserveAvailableState.SelectedClient -> {
                            mediaController.selectedClient.collect { sendData(correlationId, it, rpc.serializer) }
                        }
                        is Rpc.ToServer.MediaController.ObserveAvailableState.MediaPlayerState -> {
                            mediaController.mediaPlayerState.collect { sendData(correlationId, it, rpc.serializer) }
                        }
                    }
                    is Rpc.ToServer.MediaController.SelectClient -> {
                        mediaController.selectClient(rpc.id)
                        sendDone(correlationId)
                    }
                    is Rpc.ToServer.MediaController.PlayQueue -> {
                        mediaController.playQueue(
                            clientId = clientId,
                            queue = rpc.queue,
                            queueItemIndex = rpc.queueItemIndex
                        )
                        sendDone(correlationId)
                    }
                    is Rpc.ToServer.MediaController.Play -> {
                        mediaController.play(clientId)
                        sendDone(correlationId)
                    }
                    is Rpc.ToServer.MediaController.Pause -> {
                        mediaController.pause()
                        sendDone(correlationId)
                    }
                    is Rpc.ToServer.MediaController.Seek -> {
                        mediaController.seek(rpc.positionMs)
                        sendDone(correlationId)
                    }
                    is Rpc.ToServer.MediaController.Next -> {
                        mediaController.next(clientId)
                        sendDone(correlationId)
                    }
                    is Rpc.ToServer.MediaController.Previous -> {
                        mediaController.previous(clientId)
                        sendDone(correlationId)
                    }
                    is Rpc.ToServer.MediaController.Repeat -> {
                        mediaController.repeat()
                        sendDone(correlationId)
                    }
                    is Rpc.ToServer.MediaController.AddToQueue -> {
                        mediaController.addToQueue(clientId, rpc.items)
                        sendDone(correlationId)
                    }
                    is Rpc.ToServer.MediaController.PlayItem -> {
                        mediaController.playItem(clientId, rpc.index)
                        sendDone(correlationId)
                    }
                    is Rpc.ToServer.MediaController.ChangeItemIndex -> {
                        mediaController.changeItemIndex(
                            oldIndex = rpc.oldIndex,
                            newIndex = rpc.newIndex
                        )
                        sendDone(correlationId)
                    }
                    is Rpc.ToServer.MediaController.RemoveItem -> {
                        mediaController.removeItem(clientId, rpc.key)
                        sendDone(correlationId)
                    }
                }
                is Rpc.ToServer.Folder -> when (rpc) {
                    is Rpc.ToServer.Folder.Add -> {
                        val result = folderRepo.add(rpc.name, rpc.parentFolderId)
                        sendData(correlationId, result, rpc.serializer)
                    }
                    is Rpc.ToServer.Folder.Get -> {
                        folderRepo.get(rpc.id)
                            .map { it?.toModel() }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.Folder.SearchFolder -> {
                        folderRepo.searchFolder(
                            folderId = rpc.folderId,
                            nameQuery = rpc.nameQuery
                        ).map { it.map { it.toModel() } }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.Folder.GetPath -> {
                        folderRepo.getPath(
                            target = rpc.target,
                        ).map { it.map { it.toModel() } }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.Folder.UpdateName -> {
                        folderRepo.updateName(
                            id = rpc.id,
                            name = rpc.name
                        )
                        sendDone(correlationId)
                    }
                }
                is Rpc.ToServer.Home.GetPage -> {
                    val result = homeRepo.getPage(
                        query = rpc.query,
                        selectedTypes = rpc.selectedTypes,
                        selectedTags = rpc.selectedTags,
                        offset = rpc.offset
                    )
                    sendData(correlationId, result, rpc.serializer)
                }
                is Rpc.ToServer.ImportSessionItem -> when (rpc) {
                    is Rpc.ToServer.ImportSessionItem.Get -> {
                        importSessionItemRepo.get(rpc.id)
                            .map { it?.toModel() }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.ImportSessionItem.Search -> {
                        importSessionItemRepo.search(
                            importSessionId = rpc.importSessionId,
                            state = rpc.state,
                            titleQuery = rpc.titleQuery,
                            order = rpc.order
                        ).map { it.map { it.toModel() } }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.ImportSessionItem.UpdateState -> {
                        importSessionItemRepo.updateState(
                            id = rpc.id,
                            state = rpc.state
                        )
                        sendDone(correlationId)
                    }
                }
                is Rpc.ToServer.ImportSession -> when (rpc) {
                    is Rpc.ToServer.ImportSession.Get -> {
                        importSessionRepo.get(rpc.id)
                            .map { it?.toModel() }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.ImportSession.GetAll -> {
                        val result = importSessionRepo.getAll(
                            limit = rpc.limit,
                            offset = rpc.offset
                        ).map { it.toModel() }
                        sendData(correlationId, result, rpc.serializer)
                    }
                }
                is Rpc.ToServer.Playlist -> when (rpc) {
                    is Rpc.ToServer.Playlist.GetAll -> {
                        val result = playlistRepo.getAll(
                            limit = rpc.limit,
                            offset = rpc.offset
                        ).map { it.toModel() }
                        sendData(correlationId, result, rpc.serializer)
                    }
                    is Rpc.ToServer.Playlist.Get -> {
                        playlistRepo.get(rpc.id)
                            .map { it?.toModel() }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.Playlist.SearchFolder -> {
                        playlistRepo.searchFolder(
                            folderId = rpc.folderId,
                            nameQuery = rpc.nameQuery
                        ).map { it.map { it.toModel() } }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.Playlist.GetTrackPlaylists -> {
                        playlistRepo.getTrackPlaylists(rpc.id)
                            .map { it.map { it.toModel() } }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.Playlist.UpdateName -> {
                        playlistRepo.updateName(
                            id = rpc.id,
                            name = rpc.name
                        )
                        sendDone(correlationId)
                    }
                }
                is Rpc.ToServer.PlaylistTrackCrossRef -> when (rpc) {
                    is Rpc.ToServer.PlaylistTrackCrossRef.ChangeItemPosition -> {
                        playlistTrackCrossRefRepo.changeItemPosition(
                            playlistId = rpc.playlistId,
                            from = rpc.from,
                            to = rpc.to
                        )
                        sendDone(correlationId)
                    }
                    is Rpc.ToServer.PlaylistTrackCrossRef.Delete -> {
                        playlistTrackCrossRefRepo.delete(
                            playlistId = rpc.playlistId,
                            trackId = rpc.trackId
                        )
                        sendDone(correlationId)
                    }
                }
                is Rpc.ToServer.Setting -> when (rpc) {
                    is Rpc.ToServer.Setting.Get -> {
                        clientSettingRepo.get(clientId, rpc.name)
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.Setting.Set -> {
                        clientSettingRepo.set(clientId, rpc.setting)
                        sendDone(correlationId)
                    }
                }
                is Rpc.ToServer.SpotifyAlbum -> when (rpc) {
                    is Rpc.ToServer.SpotifyAlbum.Get -> {
                        spotifyAlbumRepo.get(
                            id = rpc.id
                        ).map { it?.toModel() }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.SpotifyAlbum.SearchArtist -> {
                        spotifyAlbumRepo.searchArtist(
                            id = rpc.id,
                            nameQuery = rpc.nameQuery
                        ).map { it.map { it.toModel() } }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                }
                is Rpc.ToServer.SpotifyArtist -> when (rpc) {
                    is Rpc.ToServer.SpotifyArtist.Get -> {
                        spotifyArtistRepo.get(rpc.id)
                            .map { it?.toModel() }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.SpotifyArtist.GetSpotifyTrackSpotifyArtists -> {
                        spotifyArtistRepo.getSpotifyTrackSpotifyArtists(rpc.id)
                            .map { it.map { it.toModel() } }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.SpotifyArtist.GetSpotifyAlbumSpotifyArtists -> {
                        spotifyArtistRepo.getSpotifyAlbumSpotifyArtists(rpc.id)
                            .map { it.map { it.toModel() } }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                }
                is Rpc.ToServer.SpotifyAuth -> when (rpc) {
                    is Rpc.ToServer.SpotifyAuth.GetAuthState -> {
                        val result = spotify.getAuthState()
                        sendData(correlationId, result, rpc.serializer)
                    }
                    is Rpc.ToServer.SpotifyAuth.Authorize -> {
                        spotify.getAuthorization(
                            clientId = rpc.clientId,
                            clientSecret = rpc.clientSecret
                        )
                        sendDone(correlationId)
                    }
                    is Rpc.ToServer.SpotifyAuth.Deauthorize -> {
                        spotify.deauthorize()
                        sendDone(correlationId)
                    }
                }
                is Rpc.ToServer.SpotifySearch.Search -> {
                    val result = spotify.search(
                        track = rpc.track,
                        artist = rpc.artist,
                        album = rpc.album,
                        year = rpc.year
                    )
                    sendData(correlationId, result, rpc.serializer)
                }
                is Rpc.ToServer.SpotifyTrack -> when (rpc) {
                    is Rpc.ToServer.SpotifyTrack.GetId -> {
                        val result = spotifyTrackRepo.getId(rpc.spotifyId)
                        sendData(correlationId, result, rpc.serializer)
                    }
                    is Rpc.ToServer.SpotifyTrack.GetAlbumTracks -> {
                        spotifyTrackRepo.getAlbumTracks(rpc.id)
                            .map { it.map { it.toModel() } }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                }
                is Rpc.ToServer.Tag -> when (rpc) {
                    is Rpc.ToServer.Tag.Get -> {
                        tagRepo.get(rpc.id)
                            .map { it?.toModel() }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.Tag.Search -> {
                        tagRepo.search(rpc.nameQuery)
                            .map { it.map { it.toModel() } }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.Tag.SearchTrackTags -> {
                        tagRepo.searchTrackTags(rpc.id, rpc.nameQuery)
                            .map { it.map { it.toModel() } }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.Tag.SearchTrackUnsetTags -> {
                        tagRepo.searchTrackUnsetTags(rpc.id, rpc.nameQuery)
                            .map { it.map { it.toModel() } }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.Tag.Add -> {
                        val result = tagRepo.add(rpc.name)
                        sendData(correlationId, result, rpc.serializer)
                    }
                }
                is Rpc.ToServer.Track -> when (rpc) {
                    is Rpc.ToServer.Track.Get -> {
                        trackRepo.get(rpc.id)
                            .map { it?.toModel() }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.Track.SearchFolder -> {
                        trackRepo.searchFolder(
                            folderId = rpc.folderId,
                            nameQuery = rpc.nameQuery
                        ).map { it.map { it.toModel() } }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.Track.SearchFolderWithTags -> {
                        trackRepo.searchFolderWithTags(
                            folderId = rpc.folderId,
                            nameQuery = rpc.nameQuery,
                            tags = rpc.tags,
                            includeUntagged = rpc.includeUntagged
                        ).map { it.map { it.toModel() } }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.Track.SearchArtistContributions -> {
                        val result = trackRepo.searchArtistContributions(
                            id = rpc.id,
                            nameQuery = rpc.nameQuery,
                            limit = rpc.limit,
                            offset = rpc.offset
                        ).map { it.toModel() }
                        sendData(correlationId, result, rpc.serializer)
                    }
                    is Rpc.ToServer.Track.SearchPlaylist -> {
                        trackRepo.searchPlaylist(
                            id = rpc.id,
                            nameQuery = rpc.nameQuery
                        ).map { it.map { it.toModel() } }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.Track.SearchWithTag -> {
                        trackRepo.searchWithTag(
                            nameQuery = rpc.nameQuery,
                            tag = rpc.tag
                        ).map { it.map { it.toModel() } }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.Track.GetPlaylistTracks -> {
                        trackRepo.getPlaylistTracks(rpc.id)
                            .map { it.map { it.toModel() } }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                    is Rpc.ToServer.Track.GetId -> {
                        val result = trackRepo.getId(rpc.spotifyId)
                        sendData(correlationId, result, rpc.serializer)
                    }
                    is Rpc.ToServer.Track.GetImportSessionTrack -> {
                        trackRepo.getImportSessionTrack(rpc.id)
                            .map { it?.toModel() }
                            .collect { sendData(correlationId, it, rpc.serializer) }
                    }
                }
                is Rpc.ToServer.PlaybackInterval.Add -> {
                    playbackIntervalManager.add(rpc.item)
                    sendDone(correlationId)
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
}