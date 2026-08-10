package dev.younesgouyd.apps.music.server.common

import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.Folder
import dev.younesgouyd.apps.music.common.models.FolderId
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.rpc.*
import dev.younesgouyd.apps.music.common.models.rpc.websocket.WsRequest
import dev.younesgouyd.apps.music.common.models.rpc.websocket.WsResponse
import dev.younesgouyd.apps.music.server.common.data.FileManager
import dev.younesgouyd.apps.music.server.common.data.repoes.*
import dev.younesgouyd.apps.music.server.common.data.room.AppDatabase
import dev.younesgouyd.apps.music.server.common.data.toModel
import dev.younesgouyd.apps.music.server.common.spotify.Spotify
import dev.younesgouyd.apps.music.server.common.usecases.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.event.Level
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class Application {
    private val logger = KotlinLogging.logger {}
    private lateinit var database: AppDatabase
    private lateinit var fileManager: FileManager
    private lateinit var folderRepo: FolderRepo
    private lateinit var playlistRepo: PlaylistRepo
    private lateinit var playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo
    private lateinit var trackRepo: TrackRepo
    private lateinit var mediaFileRepo: MediaFileRepo
    private lateinit var importSessionRepo: ImportSessionRepo
    private lateinit var importSessionItemRepo: ImportSessionItemRepo
    private lateinit var tagRepo: TagRepo
    private lateinit var tagTrackCrossRefRepo: TagTrackCrossRefRepo
    private lateinit var settingRepo: SettingRepo
    private lateinit var spotifyAlbumRepo: SpotifyAlbumRepo
    private lateinit var spotifyArtistRepo: SpotifyArtistRepo
    private lateinit var spotifyTrackRepo: SpotifyTrackRepo

    private lateinit var ytDlp: YtDlp
    private lateinit var spotify: Spotify

    private lateinit var importWorker: ImportWorker

    private lateinit var unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase
    private lateinit var setTrackMetadataFromSpotifyUseCase: SetTrackMetadataFromSpotifyUseCase
    private lateinit var clearImportItemUseCase: ClearImportItemUseCase
    private lateinit var deleteFolderUseCase: DeleteFolderUseCase
    private lateinit var prepareImportUseCase: PrepareImportUseCase

    private lateinit var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>

    fun start(homeDir: File) {
        logger.info { "--> Application::start" }
        val appDir = File(homeDir, "younesmusicserverdata").also { it.mkdir() }
        val mediaDir = File(appDir, "media").also { it.mkdir() }
        val logsDir = File(appDir, "logs").also { it.mkdir() }
        val dbDir = File(appDir, "database").also { it.mkdir() }
        val dbFile = File(dbDir, "younesmusic.db").also { if (!it.exists()) it.createNewFile() }
        val ytDlpDir = File(appDir, "ytdlp").also { it.mkdir() }
        val ytDlpFile = File(ytDlpDir, "serveraddress.txt").also { if (!it.exists()) it.createNewFile() }
        val spotifyDir = File(appDir, "spotify").also { it.mkdir() }
        val spotifyTokensFile = File(spotifyDir, "tokens").also { if (!it.exists()) it.createNewFile() }
        val spotifyCredentialsFile = File(spotifyDir, "credentials").also { if (!it.exists()) it.createNewFile() }

        database = createDatabaseInstance(dbFile)
        fileManager = FileManager(mediaDir)

        runBlocking {
            if (database.settingDao().darkThemeExists() == null) {
                val currentTime = System.currentTimeMillis()
                database.settingDao().initDarkTheme(
                    creationDatetime = currentTime,
                    updateDatetime = currentTime
                )
            }
            if (database.settingDao().spotifyClientIdExists() == null) {
                val currentTime = System.currentTimeMillis()
                database.settingDao().initSpotifyClientId(
                    creationDatetime = currentTime,
                    updateDatetime = currentTime
                )
            }
            if (database.settingDao().spotifyClientSecretExists() == null) {
                val currentTime = System.currentTimeMillis()
                database.settingDao().initSpotifyClientSecret(
                    creationDatetime = currentTime,
                    updateDatetime = currentTime
                )
            }
            if (database.settingDao().spotifyAccessTokenExists() == null) {
                val currentTime = System.currentTimeMillis()
                database.settingDao().initSpotifyAccessToken(
                    creationDatetime = currentTime,
                    updateDatetime = currentTime
                )
            }
            if (database.settingDao().spotifyTokenExpirationTimeExists() == null) {
                val currentTime = System.currentTimeMillis()
                database.settingDao().initSpotifyTokenExpirationTime(
                    creationDatetime = currentTime,
                    updateDatetime = currentTime
                )
            }
            if (database.settingDao().spotifyTokenDatetimeExists() == null) {
                val currentTime = System.currentTimeMillis()
                database.settingDao().initSpotifyTokenDatetime(
                    creationDatetime = currentTime,
                    updateDatetime = currentTime
                )
            }
        }

        folderRepo = FolderRepo(database.folderDao())
        playlistRepo = PlaylistRepo(database.playlistDao())
        playlistTrackCrossRefRepo = PlaylistTrackCrossRefRepo(database.playlistTrackCrossRefDao())
        trackRepo = TrackRepo(database.trackDao())
        mediaFileRepo = MediaFileRepo(database.mediaFileDao())
        importSessionRepo = ImportSessionRepo(database.importSessionDao())
        importSessionItemRepo = ImportSessionItemRepo(database.importSessionItemDao())
        tagRepo = TagRepo(database.tagDao())
        tagTrackCrossRefRepo = TagTrackCrossRefRepo(database.tagTrackCrossRefDao())
        settingRepo = SettingRepo(database.settingDao())
        spotifyAlbumRepo = SpotifyAlbumRepo(database.spotifyAlbumDao())
        spotifyArtistRepo = SpotifyArtistRepo(database.spotifyArtistDao())
        spotifyTrackRepo = SpotifyTrackRepo(database.spotifyTrackDao())

        ytDlp = YtDlp(ytDlpFile.readText())

        spotify = Spotify(tokensFile = spotifyTokensFile, credentialsFile = spotifyCredentialsFile)

        importWorker = ImportWorker(
            importSessionRepo = importSessionRepo,
            importSessionItemRepo = importSessionItemRepo,
            transaction = database.importTrx(),
            ytDlp = ytDlp,
            fileManager = fileManager,
        )

        unsetSpotifyTrackUseCase = UnsetSpotifyTrackUseCase(
            transaction = database.unsetSpotifyTrack(),
            fileManager = fileManager
        )
        setTrackMetadataFromSpotifyUseCase = SetTrackMetadataFromSpotifyUseCase(
            unsetSpotifyTrackUseCase = unsetSpotifyTrackUseCase,
            trackRepo = trackRepo,
            spotifyAlbumRepo = spotifyAlbumRepo,
            transaction = database.setTrackMetadataFromSpotify(),
            spotify = spotify,
            fileManager = fileManager
        )
        clearImportItemUseCase = ClearImportItemUseCase(
            unsetSpotifyTrackUseCase = unsetSpotifyTrackUseCase,
            trackRepo = trackRepo,
            transaction = database.clearImportSessionItem(),
            fileManager = fileManager
        )
        deleteFolderUseCase = DeleteFolderUseCase(
            playlistRepo = playlistRepo,
            trackRepo = trackRepo,
            clearImportItemUseCase = clearImportItemUseCase,
            folderRepo = folderRepo
        )
        prepareImportUseCase = PrepareImportUseCase(
            fileManager = fileManager,
            transaction = database.prepareImportFromInternet()
        )

        server = embeddedServer(factory = CIO, port = 8080) {
            install(CallLogging) {
                level = Level.DEBUG
                this.format { call ->
                    val status = call.response.status()
                    val httpMethod = call.request.httpMethod.value
                    val userAgent = call.request.headers["User-Agent"]
                    "Status: $status, HTTP method: $httpMethod, User agent: $userAgent"
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(json)
                pingPeriod = 15.seconds
                timeout = 15.seconds
            }
            routing {
                route("/") {
                    handle {
                        call.respond("music backend")
                    }
                }
                webSocket("rpc") {
                    val jobs = ConcurrentHashMap<Uuid, Job>()
                    try {
                        while (isActive) {
                            when (val request = receiveDeserialized<WsRequest>()) {
                                is WsRequest.Execute -> {
                                    jobs[request.correlationId]?.cancel()
                                    jobs[request.correlationId] = launch {
                                        try {
                                            when (val rpc = request.rpc) {
                                                is FolderRpc -> when (rpc) {
                                                    is FolderRpc.Add -> {
                                                        val result = folderRepo.add(rpc.name, rpc.parentFolderId)
                                                        sendSerialized<WsResponse<FolderId>>(WsResponse(request.correlationId, result))
                                                    }
                                                    is FolderRpc.Delete -> {
                                                        folderRepo.delete(rpc.id)
                                                    }
                                                    is FolderRpc.Get -> {
                                                        folderRepo.get(rpc.id)
                                                            .map { it?.toModel() }
                                                            .collect { sendSerialized<WsResponse<Folder?>>(WsResponse(request.correlationId, it)) }
                                                    }
                                                    is FolderRpc.GetSubfolders -> {
                                                        folderRepo.getSubfolders(rpc.id)
                                                            .map { it.map { it.toModel() } }
                                                            .collect { sendSerialized<WsResponse<List<Folder>>>(WsResponse(request.correlationId, it)) }
                                                    }
                                                    is FolderRpc.SearchFolder -> {
                                                        folderRepo.searchFolder(
                                                            folderId = rpc.folderId,
                                                            nameQuery = rpc.nameQuery
                                                        ).map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is FolderRpc.UpdateName -> {
                                                        folderRepo.updateName(
                                                            id = rpc.id,
                                                            name = rpc.name
                                                        )
                                                    }
                                                    is FolderRpc.UpdateParentFolderId -> {
                                                        folderRepo.updateParentFolderId(
                                                            id = rpc.id,
                                                            parentFolderId = rpc.parentFolderId
                                                        )
                                                    }
                                                }
                                                is ImportSessionItemRpc -> when (rpc) {
                                                    is ImportSessionItemRpc.Get -> {
                                                        importSessionItemRepo.get(rpc.id)
                                                            .map { it?.toModel() }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is ImportSessionItemRpc.Search -> {
                                                        importSessionItemRepo.search(
                                                            importSessionId = rpc.importSessionId,
                                                            state = rpc.state,
                                                            titleQuery = rpc.titleQuery,
                                                            order = rpc.order
                                                        ).map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is ImportSessionItemRpc.UpdateState -> {
                                                        importSessionItemRepo.updateState(
                                                            id = rpc.id,
                                                            state = rpc.state
                                                        )
                                                    }
                                                }
                                                is ImportSessionRpc -> when (rpc) {
                                                    is ImportSessionRpc.Get -> {
                                                        importSessionRepo.get(rpc.id)
                                                            .map { it?.toModel() }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is ImportSessionRpc.GetAll -> {
                                                        val result = importSessionRepo.getAll(
                                                            limit = rpc.limit,
                                                            offset = rpc.offset
                                                        ).map { it.toModel() }
                                                        sendWsResponse(request.correlationId, result)
                                                    }
                                                }
                                                is InspectionRpc.Inspect -> TODO()
                                                is MediaFileRpc -> when (rpc) {
                                                    is MediaFileRpc.GetImportSessionImage -> {
                                                        val result = mediaFileRepo.getImportSessionImage(rpc.id)
                                                        sendWsResponse(request.correlationId, result)
                                                    }
                                                    is MediaFileRpc.GetImportSessionItemImage -> {
                                                        val result = mediaFileRepo.getImportSessionItemImage(rpc.id)
                                                        sendWsResponse(request.correlationId, result)
                                                    }
                                                    is MediaFileRpc.GetSpotifyAlbumImage -> {
                                                        val result = mediaFileRepo.getSpotifyAlbumImage(rpc.id)
                                                        sendWsResponse(request.correlationId, result)
                                                    }
                                                    is MediaFileRpc.GetSpotifyArtistImage -> {
                                                        val result = mediaFileRepo.getSpotifyArtistImage(rpc.id)
                                                        sendWsResponse(request.correlationId, result)
                                                    }
                                                    is MediaFileRpc.GetImportSessionItemAudio -> {
                                                        val result = mediaFileRepo.getImportSessionItemAudio(rpc.id)
                                                        sendWsResponse(request.correlationId, result)
                                                    }
                                                }
                                                is PlaylistRpc -> when (rpc) {
                                                    is PlaylistRpc.GetAll -> {
                                                        val result = playlistRepo.getAll(
                                                            limit = rpc.limit,
                                                            offset = rpc.offset
                                                        ).map { it.toModel() }
                                                        sendWsResponse(request.correlationId, result)
                                                    }
                                                    is PlaylistRpc.Get -> {
                                                        playlistRepo.get(rpc.id)
                                                            .map { it?.toModel() }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is PlaylistRpc.Search -> {
                                                        playlistRepo.search(rpc.nameQuery)
                                                            .map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is PlaylistRpc.SearchFolder -> {
                                                        playlistRepo.searchFolder(
                                                            folderId = rpc.folderId,
                                                            nameQuery = rpc.nameQuery
                                                        ).map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is PlaylistRpc.GetFolderPlaylists -> {
                                                        playlistRepo.getFolderPlaylists(
                                                            folderId = rpc.folderId
                                                        ).map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is PlaylistRpc.GetTrackPlaylists -> {
                                                        playlistRepo.getTrackPlaylists(rpc.id)
                                                            .map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is PlaylistRpc.Add -> {
                                                        val result = playlistRepo.add(
                                                            name = rpc.name,
                                                            folderId = rpc.folderId
                                                        )
                                                        sendWsResponse(request.correlationId, result)
                                                    }
                                                    is PlaylistRpc.UpdateName -> {
                                                        playlistRepo.updateName(
                                                            id = rpc.id,
                                                            name = rpc.name
                                                        )
                                                    }
                                                    is PlaylistRpc.UpdateFolderId -> {
                                                        playlistRepo.updateFolderId(
                                                            id = rpc.id,
                                                            folderId = rpc.folderId
                                                        )
                                                    }
                                                    is PlaylistRpc.Delete -> {
                                                        playlistRepo.delete(rpc.id)
                                                    }
                                                }
                                                is PlaylistTrackCrossRefRpc -> when (rpc) {
                                                    is PlaylistTrackCrossRefRpc.Get -> {
                                                        playlistTrackCrossRefRepo.get(
                                                            playlistId = rpc.playlistId,
                                                            trackId = rpc.trackId
                                                        ).map { it?.toModel() }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is PlaylistTrackCrossRefRpc.Add -> {
                                                        playlistTrackCrossRefRepo.add(
                                                            playlistId = rpc.playlistId,
                                                            trackId = rpc.trackId
                                                        )
                                                    }
                                                    is PlaylistTrackCrossRefRpc.ChangeItemPosition -> {
                                                        playlistTrackCrossRefRepo.changeItemPosition(
                                                            playlistId = rpc.playlistId,
                                                            from = rpc.from,
                                                            to = rpc.to
                                                        )
                                                    }
                                                    is PlaylistTrackCrossRefRpc.Delete -> {
                                                        playlistTrackCrossRefRepo.delete(
                                                            playlistId = rpc.playlistId,
                                                            trackId = rpc.trackId
                                                        )
                                                    }
                                                }
                                                is SettingRpc -> when (rpc) {
                                                    is SettingRpc.GetDarkTheme -> {
                                                        settingRepo.getDarkTheme()
                                                            .map { it.toModel() }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is SettingRpc.UpdateDarkTheme -> {
                                                        settingRepo.updateDarkTheme(rpc.theme)
                                                    }
                                                }
                                                is SpotifyAlbumRpc -> when (rpc) {
                                                    is SpotifyAlbumRpc.Get -> {
                                                        spotifyAlbumRepo.get(
                                                            id = rpc.id
                                                        ).map { it?.toModel() }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is SpotifyAlbumRpc.SearchArtist -> {
                                                        spotifyAlbumRepo.searchArtist(
                                                            id = rpc.id,
                                                            nameQuery = rpc.nameQuery
                                                        ).map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                }
                                                is SpotifyArtistRpc -> when (rpc) {
                                                    is SpotifyArtistRpc.Get -> {
                                                        spotifyArtistRepo.get(rpc.id)
                                                            .map { it?.toModel() }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is SpotifyArtistRpc.Search -> {
                                                        val result = spotifyArtistRepo.search(
                                                            nameQuery = rpc.nameQuery,
                                                            limit = rpc.limit,
                                                            offset = rpc.offset
                                                        ).map { it.toModel() }
                                                        sendWsResponse(request.correlationId, result)
                                                    }
                                                    is SpotifyArtistRpc.GetSpotifyTrackSpotifyArtists -> {
                                                        spotifyArtistRepo.getSpotifyTrackSpotifyArtists(rpc.id)
                                                            .map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is SpotifyArtistRpc.GetSpotifyAlbumSpotifyArtists -> {
                                                        spotifyArtistRepo.getSpotifyAlbumSpotifyArtists(rpc.id)
                                                            .map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                }
                                                is SpotifyAuthRpc -> when (rpc) {
                                                    is SpotifyAuthRpc.GetAuthState -> {
                                                        val result = spotify.getAuthState()
                                                        sendWsResponse(request.correlationId, result)
                                                    }
                                                    is SpotifyAuthRpc.Authorize -> {
                                                        spotify.getAuthorization(
                                                            clientId = rpc.clientId,
                                                            clientSecret = rpc.clientSecret
                                                        )
                                                    }
                                                    is SpotifyAuthRpc.Deauthorize -> {
                                                        spotify.deauthorize()
                                                    }
                                                }
                                                is SpotifySearchRpc.Search -> {
                                                    val result = spotify.search(
                                                        track = rpc.track,
                                                        artist = rpc.artist,
                                                        album = rpc.album,
                                                        year = rpc.year
                                                    )
                                                    sendWsResponse(request.correlationId, result)
                                                }
                                                is SpotifyTrackRpc -> when (rpc) {
                                                    is SpotifyTrackRpc.GetId -> {
                                                        val result = spotifyTrackRepo.getId(rpc.spotifyId)
                                                        sendWsResponse(request.correlationId, result)
                                                    }
                                                    is SpotifyTrackRpc.GetAlbumTracks -> {
                                                        spotifyTrackRepo.getAlbumTracks(rpc.id)
                                                            .map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                }
                                                is TagRpc -> when (rpc) {
                                                    is TagRpc.Get -> {
                                                        tagRepo.get(rpc.id)
                                                            .map { it?.toModel() }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is TagRpc.Search -> {
                                                        tagRepo.search(rpc.nameQuery)
                                                            .map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is TagRpc.GetTrackTags -> {
                                                        tagRepo.getTrackTags(rpc.id)
                                                            .map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is TagRpc.GetTrackUnsetTags -> {
                                                        tagRepo.getTrackUnsetTags(rpc.id)
                                                            .map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is TagRpc.Add -> { tagRepo.add(rpc.name) }
                                                    is TagRpc.Delete -> { tagRepo.delete(rpc.id) }
                                                }
                                                is TagTrackCrossRefRpc -> when (rpc) {
                                                    is TagTrackCrossRefRpc.Add -> { tagTrackCrossRefRepo.add(rpc.tagId, rpc.trackId) }
                                                    is TagTrackCrossRefRpc.Delete -> { tagTrackCrossRefRepo.delete(rpc.tagId, rpc.trackId) }
                                                }
                                                is TrackRpc -> when (rpc) {
                                                    is TrackRpc.Get -> {
                                                        trackRepo.get(rpc.id)
                                                            .map { it?.toModel() }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is TrackRpc.Search -> {
                                                        val result = trackRepo.search(
                                                            nameQuery = rpc.nameQuery,
                                                            limit = rpc.limit,
                                                            offset = rpc.offset
                                                        ).map { it.toModel() }
                                                        sendWsResponse(request.correlationId, result)
                                                    }
                                                    is TrackRpc.SearchWithTags -> {
                                                        val result = trackRepo.searchWithTags(
                                                            nameQuery = rpc.nameQuery,
                                                            tags = rpc.tags,
                                                            includeUntagged = rpc.includeUntagged,
                                                            limit = rpc.limit,
                                                            offset = rpc.offset
                                                        ).map { it.toModel() }
                                                        sendWsResponse(request.correlationId, result)
                                                    }
                                                    is TrackRpc.SearchFolder -> {
                                                        trackRepo.searchFolder(
                                                            folderId = rpc.folderId,
                                                            nameQuery = rpc.nameQuery
                                                        ).map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is TrackRpc.SearchFolderWithTags -> {
                                                        trackRepo.searchFolderWithTags(
                                                            folderId = rpc.folderId,
                                                            nameQuery = rpc.nameQuery,
                                                            tags = rpc.tags,
                                                            includeUntagged = rpc.includeUntagged
                                                        ).map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is TrackRpc.SearchArtistContributions -> {
                                                        val result = trackRepo.searchArtistContributions(
                                                            id = rpc.id,
                                                            nameQuery = rpc.nameQuery,
                                                            limit = rpc.limit,
                                                            offset = rpc.offset
                                                        ).map { it.toModel() }
                                                        sendWsResponse(request.correlationId, result)
                                                    }
                                                    is TrackRpc.SearchPlaylist -> {
                                                        trackRepo.searchPlaylist(
                                                            id = rpc.id,
                                                            nameQuery = rpc.nameQuery
                                                        ).map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is TrackRpc.SearchWithTag -> {
                                                        trackRepo.searchWithTag(
                                                            nameQuery = rpc.nameQuery,
                                                            tag = rpc.tag
                                                        ).map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is TrackRpc.GetFolderTracks -> {
                                                        trackRepo.getFolderTracks(rpc.id)
                                                            .map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is TrackRpc.GetArtistTracks -> {
                                                        trackRepo.getArtistTracks(rpc.id)
                                                            .map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is TrackRpc.GetAlbumTracks -> {
                                                        trackRepo.getAlbumTracks(rpc.id)
                                                            .map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is TrackRpc.GetPlaylistTracks -> {
                                                        trackRepo.getPlaylistTracks(rpc.id)
                                                            .map { it.map { it.toModel() } }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is TrackRpc.GetId -> {
                                                        val result = trackRepo.getId(rpc.spotifyId)
                                                        sendWsResponse(request.correlationId, result)
                                                    }
                                                    is TrackRpc.GetImportSessionTrack -> {
                                                        trackRepo.getImportSessionTrack(rpc.id)
                                                            .map { it?.toModel() }
                                                            .collect { sendWsResponse(request.correlationId, it) }
                                                    }
                                                    is TrackRpc.Add -> {
                                                        val result = trackRepo.add(
                                                            importSessionItemId = rpc.importSessionItemId,
                                                            spotifyTrackId = rpc.spotifyTrackId,
                                                            folderId = rpc.folderId
                                                        )
                                                        sendWsResponse(request.correlationId, result)
                                                    }
                                                    is TrackRpc.UpdateFolderId -> {
                                                        trackRepo.updateFolderId(
                                                            id = rpc.id,
                                                            folderId = rpc.folderId
                                                        )
                                                    }
                                                }
                                                is ClearImportItemRpc -> {
                                                    clearImportItemUseCase.execute(rpc.id)
                                                }
                                                is DeleteFolderRpc -> {
                                                    deleteFolderUseCase.execute(rpc.id)
                                                }
                                                is PrepareImportRpc -> {
                                                    val result = prepareImportUseCase.execute(
                                                        selected = rpc.selected,
                                                        url = rpc.url,
                                                        inspection = rpc.inspection,
                                                        destinationFolderId = rpc.destinationFolderId
                                                    )
                                                    sendWsResponse(request.correlationId, result)
                                                }
                                                is SetTrackMetadataFromSpotifyRpc -> {
                                                    setTrackMetadataFromSpotifyUseCase.execute(
                                                        trackId = rpc.trackId,
                                                        spotifyApiTrack = rpc.spotifyApiTrack
                                                    )
                                                }
                                                is UnsetSpotifyTrackRpc -> {
                                                    unsetSpotifyTrackUseCase.execute(
                                                        trackId = rpc.trackId,
                                                        spotifyTrackId = rpc.spotifyTrackId,
                                                        spotifyAlbumId = rpc.spotifyAlbumId
                                                    )
                                                }
                                            }
                                        } finally {
                                            jobs.remove(request.correlationId)
                                        }
                                    }
                                }
                                is WsRequest.Cancel -> {
                                    jobs.remove(request.correlationId)?.cancel()
                                }
                            }
                        }
                    } finally {
                        jobs.values.forEach { it.cancel() }
                        jobs.clear()
                    }
                }
                route("files/{id}") {
                    handle {
                        val id = try {
                            MediaFileId(call.parameters["id"]!!.toLong())
                        } catch (_: Exception) {
                            return@handle call.respond(HttpStatusCode.BadRequest)
                        }
                        val file = fileManager.getMediaFile(id)
                        call.respondFile(file)
                    }
                }
            }
        }

        server.start()
        importWorker.start()
        logger.info { "<-- Application::start" }
    }

    fun stop() {
        logger.info { "--> Application::stop" }
        runBlocking {
            try { server.stopSuspend(1000, 1000) } catch (_: Exception) { }
            try { importWorker.stop() } catch (_: Exception) { }
            try { ytDlp.close() } catch (_: Exception) { }
            try { spotify.close() } catch (_: Exception) { }
            try { database.close() } catch (_: Exception) { }
        }
        logger.info { "<-- Application::stop" }
    }


    private suspend inline fun <reified T> WebSocketServerSession.sendWsResponse(correlationId: Uuid, data: T) {
        sendSerialized<WsResponse<T>>(WsResponse(correlationId, data))
    }
//    private suspend inline fun <reified T> WebSocketServerSession.sendWsResponse(response: WsResponse<T>) {
//        sendSerialized(response)
//    }
}
