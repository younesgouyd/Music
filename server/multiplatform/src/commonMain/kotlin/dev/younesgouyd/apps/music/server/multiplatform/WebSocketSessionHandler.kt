package dev.younesgouyd.apps.music.server.multiplatform

import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.rpc.*
import dev.younesgouyd.apps.music.server.multiplatform.data.RepoStore
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.ClientSettingRepo
import dev.younesgouyd.apps.music.server.multiplatform.data.toModel
import dev.younesgouyd.apps.music.server.multiplatform.spotify.Spotify
import dev.younesgouyd.apps.music.server.multiplatform.usecases.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.websocket.*
import io.ktor.utils.io.*
import io.ktor.websocket.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class WebSocketSessionHandler(
    val clientId: ClientId,
    val session: DefaultWebSocketServerSession,
    private val repoStore: RepoStore,
    private val mediaController: MediaController,
    private val ytDlp: YtDlp,
    private val spotify: Spotify,
    private val unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase,
    private val setTrackMetadataFromSpotifyUseCase: SetTrackMetadataFromSpotifyUseCase,
    private val clearImportItemUseCase: ClearImportItemUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase,
    private val prepareImportUseCase: PrepareImportUseCase
) {
    suspend fun start() {
        with(session) {
            try {
                while (isActive) {
                    val message = receiveDeserialized<WSMessage>()
                    handleIncomingMessage(message)
                }
            } finally {
                jobs.values.forEach { it.cancel() }
                jobs.clear()
                responseChannels.values.forEach { it.close() }
                responseChannels.clear()
                logger.debug { "client $clientId was disconnected" }
            }
        }
    }

    suspend inline fun <reified T> call(rpc: Rpc): T {
        val correlationId = Uuid.random()
        val channel = Channel<WSMessage.Response>(Channel.CONFLATED)
        responseChannels[correlationId] = channel
        val rpcJson: String = json.encodeToString(rpc)
        try {
            val request: WSMessage = WSMessage.Request.Execute(correlationId, rpc)
            logger.debug { "... ::call | request: $correlationId , rpc: $rpcJson"}
            session.sendSerialized<WSMessage>(request)
            return when (val response = channel.receive()) {
                is WSMessage.Response.Done -> Unit as T
                is WSMessage.Response.Data -> json.decodeFromJsonElement<T>(response.value)
                is WSMessage.Response.Failure -> TODO()
            }
        } catch (e: CancellationException) {
            logger.debug { "... ::call | CancellationException, requesting client to cancel $correlationId corresponding to RPC: $rpcJson" }
            requestCancel(correlationId)
            throw e
        } finally {
            responseChannels.remove(correlationId)
            channel.close()
        }
    }

    inline fun <reified T> observe(rpc: Rpc): Flow<T> {
        return flow {
            val activeStream = getOrCreateObservable<T>(rpc)
            emitAll(activeStream)
        }
    }

    suspend fun close() {
        session.close()
    }







    // TODO: find a way to make the non-private private
    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> PRIVATE MEMBER, DONT USE >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    val logger = KotlinLogging.logger {}
    val responseChannels = ConcurrentHashMap<Uuid, SendChannel<WSMessage.Response>>()
    private val jobs = ConcurrentHashMap<Uuid, Job>()
    val streams = ConcurrentHashMap<Rpc, SharedFlow<Any?>>()

    private val clientSettingRepo get() = repoStore.clientSettingRepo
    private val folderRepo get() = repoStore.folderRepo
    private val playlistRepo get() = repoStore.playlistRepo
    private val playlistTrackCrossRefRepo get() = repoStore.playlistTrackCrossRefRepo
    private val trackRepo get() = repoStore.trackRepo
    private val mediaFileRepo get() = repoStore.mediaFileRepo
    private val importSessionRepo get() = repoStore.importSessionRepo
    private val importSessionItemRepo get() = repoStore.importSessionItemRepo
    private val tagRepo get() = repoStore.tagRepo
    private val tagTrackCrossRefRepo get() = repoStore.tagTrackCrossRefRepo
    private val spotifyAlbumRepo get() = repoStore.spotifyAlbumRepo
    private val spotifyArtistRepo get() = repoStore.spotifyArtistRepo
    private val spotifyTrackRepo get() = repoStore.spotifyTrackRepo

    private fun DefaultWebSocketServerSession.handleIncomingMessage(message: WSMessage) {
        logger.info { "--> ::handleIncomingMessage | message: $message" }
        when (message) {
            is WSMessage.Registration -> { TODO() }
            is WSMessage.Request -> when (message) {
                is WSMessage.Request.Execute -> {
                    jobs[message.correlationId]?.cancel() // TODO
                    jobs[message.correlationId] = launch {
                        try {
                            process(
                                correlationId = message.correlationId,
                                rpc = message.rpc as? Rpc.ToServer ?: TODO()
                            )
                        } catch (e: CancellationException) {
                            logger.debug { "cancelled incoming RPC ${message.correlationId}, message.rpc: ${message.rpc}" }
                            throw e
                        } catch (e: Exception) {
                            logger.error(e) { "RPC execution failed for ${message.correlationId}" }
                        } finally {
                            jobs.remove(message.correlationId)
                        }
                    }
                }
                is WSMessage.Request.Cancel -> {
                    jobs.remove(message.correlationId)?.cancel()
                }
            }
            is WSMessage.Response -> {
                responseChannels[message.correlationId]?.trySend(message)
            }
        }
    }

    fun requestCancel(id: Uuid) {
        session.launch {
            try {
                val request: WSMessage = WSMessage.Request.Cancel(id)
                session.sendSerialized<WSMessage>(request)
            } catch (e: Exception) {
                logger.error(e) { "::requestCancel | Cancellation Request $id failed" }
            }
        }
    }

    inline fun <reified T> getOrCreateObservable(rpc: Rpc): SharedFlow<T> {
        return streams.computeIfAbsent(rpc) { key ->
            lateinit var sharedFlow: SharedFlow<T>
            sharedFlow = flow {
                val correlationId = Uuid.random()
                val channel = Channel<WSMessage.Response>(Channel.CONFLATED)
                responseChannels[correlationId] = channel
                try {
                    val request: WSMessage = WSMessage.Request.Execute(correlationId, rpc)
                    logger.debug { "... ::getOrCreateObservable | request: $correlationId , rpc: ${json.encodeToString(rpc)}"}
                    session.sendSerialized<WSMessage>(request)
                    for (state in channel) {
                        val parsed = when (state) {
                            is WSMessage.Response.Done -> TODO()
                            is WSMessage.Response.Data -> json.decodeFromJsonElement<T>(state.value)
                            is WSMessage.Response.Failure -> TODO()
                        }
                        emit(parsed)
                    }
                } finally {
                    responseChannels.remove(correlationId)
                    channel.close()
                    requestCancel(correlationId)
                }
            }.distinctUntilChanged()
                .onCompletion { streams.remove(key, sharedFlow) }
                .shareIn(
                    scope = session, // TODO
                    started = SharingStarted.WhileSubscribed(5.seconds),
                    replay = 1
                )
            sharedFlow
        } as SharedFlow<T>
    }

    private suspend fun DefaultWebSocketServerSession.process(correlationId: Uuid, rpc: Rpc.ToServer) {
        when (rpc) {
            is FolderRpc -> when (rpc) {
                is FolderRpc.Add -> {
                    val result = folderRepo.add(rpc.name, rpc.parentFolderId)
                    sendWsResponse(correlationId, result)
                }
                is FolderRpc.Delete -> {
                    folderRepo.delete(rpc.id)
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
                is FolderRpc.Get -> {
                    folderRepo.get(rpc.id)
                        .map { it?.toModel() }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is FolderRpc.GetSubfolders -> {
                    folderRepo.getSubfolders(rpc.id)
                        .map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is FolderRpc.SearchFolder -> {
                    folderRepo.searchFolder(
                        folderId = rpc.folderId,
                        nameQuery = rpc.nameQuery
                    ).map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is FolderRpc.UpdateName -> {
                    folderRepo.updateName(
                        id = rpc.id,
                        name = rpc.name
                    )
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
                is FolderRpc.UpdateParentFolderId -> {
                    folderRepo.updateParentFolderId(
                        id = rpc.id,
                        parentFolderId = rpc.parentFolderId
                    )
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
            }
            is ImportSessionItemRpc -> when (rpc) {
                is ImportSessionItemRpc.Get -> {
                    importSessionItemRepo.get(rpc.id)
                        .map { it?.toModel() }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is ImportSessionItemRpc.Search -> {
                    importSessionItemRepo.search(
                        importSessionId = rpc.importSessionId,
                        state = rpc.state,
                        titleQuery = rpc.titleQuery,
                        order = rpc.order
                    ).map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is ImportSessionItemRpc.UpdateState -> {
                    importSessionItemRepo.updateState(
                        id = rpc.id,
                        state = rpc.state
                    )
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
            }
            is ImportSessionRpc -> when (rpc) {
                is ImportSessionRpc.Get -> {
                    importSessionRepo.get(rpc.id)
                        .map { it?.toModel() }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is ImportSessionRpc.GetAll -> {
                    val result = importSessionRepo.getAll(
                        limit = rpc.limit,
                        offset = rpc.offset
                    ).map { it.toModel() }
                    sendWsResponse(correlationId, result)
                }
            }
            is InspectionRpc.Inspect -> {
                try {
                    val result = ytDlp.inspect(rpc.url)
                    sendWsResponse(correlationId, result)
                } catch (e: Throwable) {
                    logger.error(e) {}
                    sendSerialized<WSMessage>(WSMessage.Response.Failure(correlationId))
                }
            }
            is MediaFileRpc -> when (rpc) {
                is MediaFileRpc.GetImportSessionImage -> {
                    val result = mediaFileRepo.getImportSessionImage(rpc.id)
                    sendWsResponse(correlationId, result)
                }
                is MediaFileRpc.GetImportSessionItemImage -> {
                    val result = mediaFileRepo.getImportSessionItemImage(rpc.id)
                    sendWsResponse(correlationId, result)
                }
                is MediaFileRpc.GetSpotifyAlbumImage -> {
                    val result = mediaFileRepo.getSpotifyAlbumImage(rpc.id)
                    sendWsResponse(correlationId, result)
                }
                is MediaFileRpc.GetSpotifyArtistImage -> {
                    val result = mediaFileRepo.getSpotifyArtistImage(rpc.id)
                    sendWsResponse(correlationId, result)
                }
                is MediaFileRpc.GetImportSessionItemAudio -> {
                    val result = mediaFileRepo.getImportSessionItemAudio(rpc.id)
                    sendWsResponse(correlationId, result)
                }
            }
            is PlaylistRpc -> when (rpc) {
                is PlaylistRpc.GetAll -> {
                    val result = playlistRepo.getAll(
                        limit = rpc.limit,
                        offset = rpc.offset
                    ).map { it.toModel() }
                    sendWsResponse(correlationId, result)
                }
                is PlaylistRpc.Get -> {
                    playlistRepo.get(rpc.id)
                        .map { it?.toModel() }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is PlaylistRpc.Search -> {
                    playlistRepo.search(rpc.nameQuery)
                        .map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is PlaylistRpc.SearchFolder -> {
                    playlistRepo.searchFolder(
                        folderId = rpc.folderId,
                        nameQuery = rpc.nameQuery
                    ).map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is PlaylistRpc.GetFolderPlaylists -> {
                    playlistRepo.getFolderPlaylists(
                        folderId = rpc.folderId
                    ).map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is PlaylistRpc.GetTrackPlaylists -> {
                    playlistRepo.getTrackPlaylists(rpc.id)
                        .map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is PlaylistRpc.Add -> {
                    val result = playlistRepo.add(
                        name = rpc.name,
                        folderId = rpc.folderId
                    )
                    sendWsResponse(correlationId, result)
                }
                is PlaylistRpc.UpdateName -> {
                    playlistRepo.updateName(
                        id = rpc.id,
                        name = rpc.name
                    )
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
                is PlaylistRpc.UpdateFolderId -> {
                    playlistRepo.updateFolderId(
                        id = rpc.id,
                        folderId = rpc.folderId
                    )
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
                is PlaylistRpc.Delete -> {
                    playlistRepo.delete(rpc.id)
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
            }
            is PlaylistTrackCrossRefRpc -> when (rpc) {
                is PlaylistTrackCrossRefRpc.Get -> {
                    playlistTrackCrossRefRepo.get(
                        playlistId = rpc.playlistId,
                        trackId = rpc.trackId
                    ).map { it?.toModel() }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is PlaylistTrackCrossRefRpc.Add -> {
                    playlistTrackCrossRefRepo.add(
                        playlistId = rpc.playlistId,
                        trackId = rpc.trackId
                    )
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
                is PlaylistTrackCrossRefRpc.ChangeItemPosition -> {
                    playlistTrackCrossRefRepo.changeItemPosition(
                        playlistId = rpc.playlistId,
                        from = rpc.from,
                        to = rpc.to
                    )
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
                is PlaylistTrackCrossRefRpc.Delete -> {
                    playlistTrackCrossRefRepo.delete(
                        playlistId = rpc.playlistId,
                        trackId = rpc.trackId
                    )
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
            }
            is SettingRpc -> when (rpc) {
                is SettingRpc.GetDarkTheme -> {
                    clientSettingRepo.get(clientId, ClientSettingRepo.SettingName.DARK_MODE)
                        .map { it?.toModel() }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is SettingRpc.UpdateDarkTheme -> {
                    logger.debug { "::process | updating dark mode" }
                    clientSettingRepo.set(clientId, ClientSettingRepo.SettingName.DARK_MODE, rpc.theme.name)
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
            }
            is SpotifyAlbumRpc -> when (rpc) {
                is SpotifyAlbumRpc.Get -> {
                    spotifyAlbumRepo.get(
                        id = rpc.id
                    ).map { it?.toModel() }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is SpotifyAlbumRpc.SearchArtist -> {
                    spotifyAlbumRepo.searchArtist(
                        id = rpc.id,
                        nameQuery = rpc.nameQuery
                    ).map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
            }
            is SpotifyArtistRpc -> when (rpc) {
                is SpotifyArtistRpc.Get -> {
                    spotifyArtistRepo.get(rpc.id)
                        .map { it?.toModel() }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is SpotifyArtistRpc.Search -> {
                    val result = spotifyArtistRepo.search(
                        nameQuery = rpc.nameQuery,
                        limit = rpc.limit,
                        offset = rpc.offset
                    ).map { it.toModel() }
                    sendWsResponse(correlationId, result)
                }
                is SpotifyArtistRpc.GetSpotifyTrackSpotifyArtists -> {
                    spotifyArtistRepo.getSpotifyTrackSpotifyArtists(rpc.id)
                        .map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is SpotifyArtistRpc.GetSpotifyAlbumSpotifyArtists -> {
                    spotifyArtistRepo.getSpotifyAlbumSpotifyArtists(rpc.id)
                        .map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
            }
            is SpotifyAuthRpc -> when (rpc) {
                is SpotifyAuthRpc.GetAuthState -> {
                    val result = spotify.getAuthState()
                    sendWsResponse(correlationId, result)
                }
                is SpotifyAuthRpc.Authorize -> {
                    spotify.getAuthorization(
                        clientId = rpc.clientId,
                        clientSecret = rpc.clientSecret
                    )
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
                is SpotifyAuthRpc.Deauthorize -> {
                    spotify.deauthorize()
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
            }
            is SpotifySearchRpc.Search -> {
                val result = spotify.search(
                    track = rpc.track,
                    artist = rpc.artist,
                    album = rpc.album,
                    year = rpc.year
                )
                sendWsResponse(correlationId, result)
            }
            is SpotifyTrackRpc -> when (rpc) {
                is SpotifyTrackRpc.GetId -> {
                    val result = spotifyTrackRepo.getId(rpc.spotifyId)
                    sendWsResponse(correlationId, result)
                }
                is SpotifyTrackRpc.GetAlbumTracks -> {
                    spotifyTrackRepo.getAlbumTracks(rpc.id)
                        .map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
            }
            is TagRpc -> when (rpc) {
                is TagRpc.Get -> {
                    tagRepo.get(rpc.id)
                        .map { it?.toModel() }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is TagRpc.Search -> {
                    tagRepo.search(rpc.nameQuery)
                        .map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is TagRpc.GetTrackTags -> {
                    tagRepo.getTrackTags(rpc.id)
                        .map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is TagRpc.GetTrackUnsetTags -> {
                    tagRepo.getTrackUnsetTags(rpc.id)
                        .map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is TagRpc.Add -> {
                    tagRepo.add(rpc.name)
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }

                is TagRpc.Delete -> {
                    tagRepo.delete(rpc.id)
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
            }
            is TagTrackCrossRefRpc -> when (rpc) {
                is TagTrackCrossRefRpc.Add -> {
                    tagTrackCrossRefRepo.add(rpc.tagId, rpc.trackId)
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }

                is TagTrackCrossRefRpc.Delete -> {
                    tagTrackCrossRefRepo.delete(rpc.tagId, rpc.trackId)
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
            }
            is TrackRpc -> when (rpc) {
                is TrackRpc.Get -> {
                    trackRepo.get(rpc.id)
                        .map { it?.toModel() }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is TrackRpc.Search -> {
                    val result = trackRepo.search(
                        nameQuery = rpc.nameQuery,
                        limit = rpc.limit,
                        offset = rpc.offset
                    ).map { it.toModel() }
                    sendWsResponse(correlationId, result)
                }
                is TrackRpc.SearchWithTags -> {
                    val result = trackRepo.searchWithTags(
                        nameQuery = rpc.nameQuery,
                        tags = rpc.tags,
                        includeUntagged = rpc.includeUntagged,
                        limit = rpc.limit,
                        offset = rpc.offset
                    ).map { it.toModel() }
                    sendWsResponse(correlationId, result)
                }
                is TrackRpc.SearchFolder -> {
                    trackRepo.searchFolder(
                        folderId = rpc.folderId,
                        nameQuery = rpc.nameQuery
                    ).map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is TrackRpc.SearchFolderWithTags -> {
                    trackRepo.searchFolderWithTags(
                        folderId = rpc.folderId,
                        nameQuery = rpc.nameQuery,
                        tags = rpc.tags,
                        includeUntagged = rpc.includeUntagged
                    ).map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is TrackRpc.SearchArtistContributions -> {
                    val result = trackRepo.searchArtistContributions(
                        id = rpc.id,
                        nameQuery = rpc.nameQuery,
                        limit = rpc.limit,
                        offset = rpc.offset
                    ).map { it.toModel() }
                    sendWsResponse(correlationId, result)
                }
                is TrackRpc.SearchPlaylist -> {
                    trackRepo.searchPlaylist(
                        id = rpc.id,
                        nameQuery = rpc.nameQuery
                    ).map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is TrackRpc.SearchWithTag -> {
                    trackRepo.searchWithTag(
                        nameQuery = rpc.nameQuery,
                        tag = rpc.tag
                    ).map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is TrackRpc.GetFolderTracks -> {
                    trackRepo.getFolderTracks(rpc.id)
                        .map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is TrackRpc.GetArtistTracks -> {
                    trackRepo.getArtistTracks(rpc.id)
                        .map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is TrackRpc.GetAlbumTracks -> {
                    trackRepo.getAlbumTracks(rpc.id)
                        .map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is TrackRpc.GetPlaylistTracks -> {
                    trackRepo.getPlaylistTracks(rpc.id)
                        .map { it.map { it.toModel() } }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is TrackRpc.GetId -> {
                    val result = trackRepo.getId(rpc.spotifyId)
                    sendWsResponse(correlationId, result)
                }
                is TrackRpc.GetImportSessionTrack -> {
                    trackRepo.getImportSessionTrack(rpc.id)
                        .map { it?.toModel() }
                        .collect { sendWsResponse(correlationId, it) }
                }
                is TrackRpc.Add -> {
                    val result = trackRepo.add(
                        importSessionItemId = rpc.importSessionItemId,
                        spotifyTrackId = rpc.spotifyTrackId,
                        folderId = rpc.folderId
                    )
                    sendWsResponse(correlationId, result)
                }
                is TrackRpc.UpdateFolderId -> {
                    trackRepo.updateFolderId(
                        id = rpc.id,
                        folderId = rpc.folderId
                    )
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
            }
            is ClearImportItemRpc -> {
                clearImportItemUseCase.execute(rpc.id)
                sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
            }
            is DeleteFolderRpc -> {
                deleteFolderUseCase.execute(rpc.id)
                sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
            }
            is PrepareImportRpc -> {
                val result = prepareImportUseCase.execute(
                    selected = rpc.selected,
                    url = rpc.url,
                    inspection = rpc.inspection,
                    destinationFolderId = rpc.destinationFolderId
                )
                sendWsResponse(correlationId, result)
            }
            is SetTrackMetadataFromSpotifyRpc -> {
                setTrackMetadataFromSpotifyUseCase.execute(
                    trackId = rpc.trackId,
                    spotifyApiTrack = rpc.spotifyApiTrack
                )
                sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
            }
            is UnsetSpotifyTrackRpc -> {
                unsetSpotifyTrackUseCase.execute(
                    trackId = rpc.trackId,
                    spotifyTrackId = rpc.spotifyTrackId,
                    spotifyAlbumId = rpc.spotifyAlbumId
                )
                sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
            }
            is MediaControllerRpc -> when (rpc) {
                is MediaControllerRpc.ObserveState ->  {
                    mediaController.state.collect { sendWsResponse(correlationId, it) }
                }
                is MediaControllerRpc.ObserveAvailableState -> when (rpc) {
                    is MediaControllerRpc.ObserveAvailableState.Clients -> {
                        mediaController.clients.collect { sendWsResponse(correlationId, it) }
                    }
                    is MediaControllerRpc.ObserveAvailableState.Enabled -> {
                        mediaController.enabled.collect { sendWsResponse(correlationId, it) }
                    }
                    is MediaControllerRpc.ObserveAvailableState.Queue -> {
                        mediaController.queue.collect { sendWsResponse(correlationId, it) }
                    }
                    is MediaControllerRpc.ObserveAvailableState.QueueItemIndex -> {
                        mediaController.currentItemIndex.collect { sendWsResponse(correlationId, it) }
                    }
                    is MediaControllerRpc.ObserveAvailableState.RepeatState -> {
                        mediaController.repeatState.collect { sendWsResponse(correlationId, it) }
                    }
                    is MediaControllerRpc.ObserveAvailableState.SelectedClient -> {
                        mediaController.selectedClient.collect { sendWsResponse(correlationId, it) }
                    }
                    is MediaControllerRpc.ObserveAvailableState.MediaPlayerState -> {
                        mediaController.mediaPlayerState.collect { sendWsResponse(correlationId, it) }
                    }
                }
                is MediaControllerRpc.SelectClient -> {
                    mediaController.selectClient(rpc.id)
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
                is MediaControllerRpc.PlayQueue -> {
                    mediaController.playQueue(
                        clientId = clientId,
                        queue = rpc.queue,
                        queueItemIndex = rpc.queueItemIndex
                    )
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
                is MediaControllerRpc.Play -> {
                    mediaController.play(clientId)
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
                is MediaControllerRpc.Pause -> {
                    mediaController.pause()
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
                is MediaControllerRpc.Seek -> {
                    mediaController.seek(rpc.positionMs.milliseconds)
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
                is MediaControllerRpc.Next -> {
                    mediaController.next(clientId)
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
                is MediaControllerRpc.Previous -> {
                    mediaController.previous(clientId)
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
                is MediaControllerRpc.Repeat -> {
                    mediaController.repeat()
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
                is MediaControllerRpc.AddToQueue -> {
                    mediaController.addToQueue(clientId, rpc.items)
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
                is MediaControllerRpc.PlayItem -> {
                    mediaController.playItem(clientId, rpc.index)
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
                is MediaControllerRpc.ChangeItemIndex -> {
                    mediaController.changeItemIndex(
                        oldIndex = rpc.oldIndex,
                        newIndex = rpc.newIndex
                    )
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
                is MediaControllerRpc.RemoveItem -> {
                    mediaController.removeItem(clientId, rpc.key)
                    sendSerialized<WSMessage>(WSMessage.Response.Done(correlationId))
                }
            }
        }
    }

    // TODO: make this private
    suspend inline fun <reified T> DefaultWebSocketServerSession.sendWsResponse(correlationId: Uuid, data: T) {
        sendSerialized<WSMessage>(
            WSMessage.Response.Data(
                correlationId = correlationId,
                value = json.encodeToJsonElement<T>(data)
            )
        )
    }
    // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< PRIVATE MEMBER, DONT USE <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
}