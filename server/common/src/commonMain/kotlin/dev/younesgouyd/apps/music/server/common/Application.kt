package dev.younesgouyd.apps.music.server.common

import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.rpc.*
import dev.younesgouyd.apps.music.common.models.spotify.SearchResult
import dev.younesgouyd.apps.music.server.common.data.FileManager
import dev.younesgouyd.apps.music.server.common.data.repoes.*
import dev.younesgouyd.apps.music.server.common.data.room.AppDatabase
import dev.younesgouyd.apps.music.server.common.data.toModel
import dev.younesgouyd.apps.music.server.common.spotify.Spotify
import dev.younesgouyd.apps.music.server.common.usecases.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.slf4j.event.Level
import java.io.File

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
        mediaFileRepo = MediaFileRepo(database.mediaFileDao(), fileManager)
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
            routing {
                route("/") {
                    handle {
                        call.respond("music backend")
                    }
                }
                route("rpc") {
                    handle {
                        val rpc = call.receive<Rpc>()
                        call.response.header("X-RPC", rpc::class.qualifiedName!!)
                        when (rpc) {
                            is FolderRpc -> when (rpc) {
                                is FolderRpc.Add -> {
                                    val result = folderRepo.add(
                                        name = rpc.name,
                                        parentFolderId = rpc.parentFolderId
                                    )
                                    call.respond<FolderId>(result)
                                }
                                is FolderRpc.Delete -> {
                                    folderRepo.delete(rpc.id)
                                }
                                is FolderRpc.Get -> {
                                    val result = folderRepo.get(rpc.id).first()?.toModel()
                                    call.respondNullable<Folder?>(result)
                                }
                                is FolderRpc.GetSubfolders -> {
                                    val result = folderRepo.getSubfolders(rpc.id).first().map { it.toModel() }
                                    call.respond<List<Folder>>(result)
                                }
                                is FolderRpc.SearchFolder -> {
                                    val result = folderRepo.searchFolder(
                                        folderId = rpc.folderId,
                                        nameQuery = rpc.nameQuery
                                    ).first().map { it.toModel() }
                                    call.respond<List<Folder>>(result)
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
                                    val result = importSessionItemRepo.get(rpc.id).first()?.toModel()
                                    call.respondNullable<ImportSessionItem?>(result)
                                }
                                is ImportSessionItemRpc.Search -> {
                                    val result = importSessionItemRepo.search(
                                        importSessionId = rpc.importSessionId,
                                        state = rpc.state,
                                        titleQuery = rpc.titleQuery,
                                        order = rpc.order
                                    ).first().map { it.toModel() }
                                    call.respond<List<ImportSessionItem>>(result)
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
                                    val result = importSessionRepo.get(rpc.id).first()?.toModel()
                                    call.respondNullable<ImportSession?>(result)
                                }
                                is ImportSessionRpc.GetAll -> {
                                    val result = importSessionRepo.getAll(
                                        limit = rpc.limit,
                                        offset = rpc.offset
                                    ).map { it.toModel() }
                                    call.respond<List<ImportSession>>(result)
                                }
                            }
                            is InspectionRpc.Inspect -> TODO()
                            is MediaFileRpc -> when (rpc) {
                                is MediaFileRpc.GetImportSessionImage -> {
                                    val result = mediaFileRepo.getImportSessionImage(rpc.id)
                                    if (result != null) {
                                        call.response.header("X-MEDIA-FILE-ID", result.first.toString())
                                        call.respondFile(result.second)
                                    } else {
                                        call.respond(HttpStatusCode.NotFound)
                                    }
                                }
                                is MediaFileRpc.GetImportSessionItemImage -> {
                                    val result = mediaFileRepo.getImportSessionItemImage(rpc.id)
                                    if (result != null) {
                                        call.response.header("X-MEDIA-FILE-ID", result.first.toString())
                                        call.respondFile(result.second)
                                    } else {
                                        call.respond(HttpStatusCode.NotFound)
                                    }
                                }
                                is MediaFileRpc.GetSpotifyAlbumImage -> {
                                    val result = mediaFileRepo.getSpotifyAlbumImage(rpc.id)
                                    if (result != null) {
                                        call.response.header("X-MEDIA-FILE-ID", result.first.toString())
                                        call.respondFile(result.second)
                                    } else {
                                        call.respond(HttpStatusCode.NotFound)
                                    }
                                }
                                is MediaFileRpc.GetSpotifyArtistImage -> {
                                    val result = mediaFileRepo.getSpotifyArtistImage(rpc.id)
                                    if (result != null) {
                                        call.response.header("X-MEDIA-FILE-ID", result.first.toString())
                                        call.respondFile(result.second)
                                    } else {
                                        call.respond(HttpStatusCode.NotFound)
                                    }
                                }
                                is MediaFileRpc.GetImportSessionItemAudio -> {
                                    val result = mediaFileRepo.getImportSessionItemAudio(rpc.id)
                                    if (result != null) {
                                        call.response.header("X-MEDIA-FILE-ID", result.first.toString())
                                        call.respondFile(result.second)
                                    } else {
                                        call.respond(HttpStatusCode.NotFound)
                                    }
                                }
                            }
                            is PlaylistRpc -> when (rpc) {
                                is PlaylistRpc.GetAll -> {
                                    val result = playlistRepo.getAll(
                                        limit = rpc.limit,
                                        offset = rpc.offset
                                    ).map { it.toModel() }
                                    call.respond<List<Playlist>>(result)
                                }
                                is PlaylistRpc.Get -> {
                                    val result = playlistRepo.get(rpc.id).first()?.toModel()
                                    call.respondNullable<Playlist?>(result)
                                }
                                is PlaylistRpc.Search -> {
                                    val result = playlistRepo.search(
                                        nameQuery = rpc.nameQuery
                                    ).first().map { it.toModel() }
                                    call.respond<List<Playlist>>(result)
                                }
                                is PlaylistRpc.SearchFolder -> {
                                    val result = playlistRepo.searchFolder(
                                        folderId = rpc.folderId,
                                        nameQuery = rpc.nameQuery
                                    ).first().map { it.toModel() }
                                    call.respond<List<Playlist>>(result)
                                }
                                is PlaylistRpc.GetFolderPlaylists -> {
                                    val result = playlistRepo.getFolderPlaylists(
                                        folderId = rpc.folderId
                                    ).first().map { it.toModel() }
                                    call.respond<List<Playlist>>(result)
                                }
                                is PlaylistRpc.GetTrackPlaylists -> {
                                    val result = playlistRepo.getTrackPlaylists(
                                        id = rpc.id
                                    ).first().map { it.toModel() }
                                    call.respond<List<Playlist>>(result)
                                }
                                is PlaylistRpc.Add -> {
                                    val result = playlistRepo.add(
                                        name = rpc.name,
                                        folderId = rpc.folderId
                                    )
                                    call.respond<PlaylistId>(result)
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
                                    val result = playlistTrackCrossRefRepo.get(
                                        playlistId = rpc.playlistId,
                                        trackId = rpc.trackId
                                    ).first()?.toModel()
                                    call.respondNullable<PlaylistTrackCrossRef?>(result)
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
                                    val result = settingRepo.getDarkTheme().first().toModel()
                                    call.respond<Setting>(result)
                                }
                                is SettingRpc.UpdateDarkTheme -> {
                                    settingRepo.updateDarkTheme(
                                        theme = rpc.theme
                                    )
                                }
                            }
                            is SpotifyAlbumRpc -> when (rpc) {
                                is SpotifyAlbumRpc.Get -> {
                                    val result = spotifyAlbumRepo.get(
                                        id = rpc.id
                                    ).first()?.toModel()
                                    call.respondNullable<SpotifyAlbum?>(result)
                                }
                                is SpotifyAlbumRpc.SearchArtist -> {
                                    val result = spotifyAlbumRepo.searchArtist(
                                        id = rpc.id,
                                        nameQuery = rpc.nameQuery
                                    ).first().map { it.toModel() }
                                    call.respond<List<SpotifyAlbum>>(result)
                                }
                            }
                            is SpotifyArtistRpc -> when (rpc) {
                                is SpotifyArtistRpc.Get -> {
                                    val result = spotifyArtistRepo.get(
                                        id = rpc.id
                                    ).first()?.toModel()
                                    call.respondNullable<SpotifyArtist?>(result)
                                }
                                is SpotifyArtistRpc.Search -> {
                                    val result = spotifyArtistRepo.search(
                                        nameQuery = rpc.nameQuery,
                                        limit = rpc.limit,
                                        offset = rpc.offset
                                    ).map { it.toModel() }
                                    call.respond<List<SpotifyArtist>>(result)
                                }
                                is SpotifyArtistRpc.GetSpotifyTrackSpotifyArtists -> {
                                    val result = spotifyArtistRepo.getSpotifyTrackSpotifyArtists(
                                        id = rpc.id
                                    ).first().map { it.toModel() }
                                    call.respond<List<SpotifyArtist>>(result)
                                }
                                is SpotifyArtistRpc.GetSpotifyAlbumSpotifyArtists -> {
                                    val result = spotifyArtistRepo.getSpotifyAlbumSpotifyArtists(
                                        id = rpc.id
                                    ).first().map { it.toModel() }
                                    call.respond<List<SpotifyArtist>>(result)
                                }
                            }
                            is SpotifyAuthRpc -> when (rpc) {
                                is SpotifyAuthRpc.GetAuthState -> {
                                    call.respond<SpotifyAuthState>(spotify.getAuthState())
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
                                call.respond<SearchResult>(result)
                            }
                            is SpotifyTrackRpc -> when (rpc) {
                                is SpotifyTrackRpc.GetId -> {
                                    val result = spotifyTrackRepo.getId(rpc.spotifyId)
                                    call.respondNullable<SpotifyTrackId?>(result)
                                }
                                is SpotifyTrackRpc.GetAlbumTracks -> {
                                    val result = spotifyTrackRepo.getAlbumTracks(rpc.id)
                                        .first()
                                        .map { it.toModel() }
                                    call.respond<List<SpotifyTrackRelation>>(result)
                                }
                            }
                            is TagRpc -> when (rpc) {
                                is TagRpc.Get -> {
                                    val result = tagRepo.get(rpc.id).first()?.toModel()
                                    call.respondNullable<Tag?>(result)
                                }
                                is TagRpc.Search -> {
                                    val result = tagRepo.search(rpc.nameQuery).first().map { it.toModel() }
                                    call.respond<List<Tag>>(result)
                                }
                                is TagRpc.GetTrackTags -> {
                                    val result = tagRepo.getTrackTags(rpc.id).first().map { it.toModel() }
                                    call.respond<List<Tag>>(result)
                                }
                                is TagRpc.GetTrackUnsetTags -> {
                                    val result = tagRepo.getTrackUnsetTags(rpc.id).first().map { it.toModel() }
                                    call.respond<List<Tag>>(result)
                                }
                                is TagRpc.Add -> { tagRepo.add(rpc.name) }
                                is TagRpc.Delete -> { tagRepo.delete(rpc.id) }
                            }
                            is TagTrackCrossRefRpc -> when (rpc) {
                                is TagTrackCrossRefRpc.Add -> { tagTrackCrossRefRepo.add(rpc.tagId, rpc.trackId) }
                                is TagTrackCrossRefRpc.Delete -> { tagTrackCrossRefRepo.delete(rpc.tagId, rpc.trackId)}
                            }
                            is TrackRpc -> when (rpc) {
                                is TrackRpc.Get -> {
                                    val result = trackRepo.get(rpc.id).first()?.toModel()
                                    call.respondNullable(result)
                                }
                                is TrackRpc.Search -> {
                                    val result = trackRepo.search(
                                        nameQuery = rpc.nameQuery,
                                        limit = rpc.limit,
                                        offset = rpc.offset
                                    ).map { it.toModel() }
                                    call.respond<List<TrackRelation>>(result)
                                }
                                is TrackRpc.SearchWithTags -> {
                                    val result = trackRepo.searchWithTags(
                                        nameQuery = rpc.nameQuery,
                                        tags = rpc.tags,
                                        includeUntagged = rpc.includeUntagged,
                                        limit = rpc.limit,
                                        offset = rpc.offset
                                    ).map { it.toModel() }
                                    call.respond<List<TrackRelation>>(result)
                                }
                                is TrackRpc.SearchFolder -> {
                                    val result = trackRepo.searchFolder(
                                        folderId = rpc.folderId,
                                        nameQuery = rpc.nameQuery
                                    ).first().map { it.toModel() }
                                    call.respond<List<TrackRelation>>(result)
                                }
                                is TrackRpc.SearchFolderWithTags -> {
                                    val result = trackRepo.searchFolderWithTags(
                                        folderId = rpc.folderId,
                                        nameQuery = rpc.nameQuery,
                                        tags = rpc.tags,
                                        includeUntagged = rpc.includeUntagged
                                    ).first().map { it.toModel() }
                                    call.respond<List<TrackRelation>>(result)
                                }
                                is TrackRpc.SearchArtistContributions -> {
                                    val result = trackRepo.searchArtistContributions(
                                        id = rpc.id,
                                        nameQuery = rpc.nameQuery,
                                        limit = rpc.limit,
                                        offset = rpc.offset
                                    ).map { it.toModel() }
                                    call.respond<List<TrackRelation>>(result)
                                }
                                is TrackRpc.SearchPlaylist -> {
                                    val result = trackRepo.searchPlaylist(
                                        id = rpc.id,
                                        nameQuery = rpc.nameQuery
                                    ).first().map { it.toModel() }
                                    call.respond<List<PlaylistTrack>>(result)
                                }
                                is TrackRpc.SearchWithTag -> {
                                    val result = trackRepo.searchWithTag(
                                        nameQuery = rpc.nameQuery,
                                        tag = rpc.tag
                                    ).first().map { it.toModel() }
                                    call.respond<List<TrackRelation>>(result)
                                }
                                is TrackRpc.GetFolderTracks -> {
                                    val result = trackRepo.getFolderTracks(rpc.id).first().map { it.toModel() }
                                    call.respond<List<Track>>(result)
                                }
                                is TrackRpc.GetArtistTracks -> {
                                    val result = trackRepo.getArtistTracks(rpc.id).first().map { it.toModel() }
                                    call.respond<List<TrackRelation>>(result)
                                }
                                is TrackRpc.GetAlbumTracks -> {
                                    val result = trackRepo.getAlbumTracks(rpc.id).first().map { it.toModel() }
                                    call.respond<List<TrackRelation>>(result)
                                }
                                is TrackRpc.GetPlaylistTracks -> {
                                    val result = trackRepo.getPlaylistTracks(rpc.id).first().map { it.toModel() }
                                    call.respond<List<TrackRelation>>(result)
                                }
                                is TrackRpc.GetId -> {
                                    val result = trackRepo.getId(rpc.spotifyId)
                                    call.respondNullable<TrackId?>(result)
                                }
                                is TrackRpc.GetImportSessionTrack -> {
                                    val result = trackRepo.getImportSessionTrack(rpc.id).first()?.toModel()
                                    call.respondNullable<TrackRelation?>(result)
                                }
                                is TrackRpc.Add -> {
                                    val result = trackRepo.add(
                                        importSessionItemId = rpc.importSessionItemId,
                                        spotifyTrackId = rpc.spotifyTrackId,
                                        folderId = rpc.folderId
                                    )
                                    call.respond<TrackId>(result)
                                }
                                is TrackRpc.UpdateFolderId -> {
                                    trackRepo.updateFolderId(
                                        id = rpc.id,
                                        folderId = rpc.folderId
                                    )
                                }
                            }
                        }
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
}
