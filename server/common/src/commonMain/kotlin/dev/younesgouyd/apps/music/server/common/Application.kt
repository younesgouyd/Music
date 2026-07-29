package dev.younesgouyd.apps.music.server.common

import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.server.common.data.FileManager
import dev.younesgouyd.apps.music.server.common.data.YtDlp
import dev.younesgouyd.apps.music.server.common.data.repoes.*
import dev.younesgouyd.apps.music.server.common.data.room.AppDatabase
import dev.younesgouyd.apps.music.server.common.spotify.Spotify
import dev.younesgouyd.apps.music.server.common.usecases.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
    private lateinit var settingsRepo: SettingsRepo
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

        folderRepo = FolderRepo(database.folderDao())
        playlistRepo = PlaylistRepo(database.playlistDao())
        playlistTrackCrossRefRepo = PlaylistTrackCrossRefRepo(database.playlistTrackCrossRefDao())
        trackRepo = TrackRepo(database.trackDao())
        mediaFileRepo = MediaFileRepo(database.mediaFileDao(), fileManager)
        importSessionRepo = ImportSessionRepo(database.importSessionDao())
        importSessionItemRepo = ImportSessionItemRepo(database.importSessionItemDao())
        tagRepo = TagRepo(database.tagDao())
        tagTrackCrossRefRepo = TagTrackCrossRefRepo(database.tagTrackCrossRefDao())
        settingsRepo = SettingsRepo(database.settingDao())
        spotifyAlbumRepo = SpotifyAlbumRepo(database.spotifyAlbumDao())
        spotifyArtistRepo = SpotifyArtistRepo(database.spotifyArtistDao())
        spotifyTrackRepo = SpotifyTrackRepo(database.spotifyTrackDao())

        ytDlp = YtDlp(ytDlpFile.readText())

        spotify = Spotify(
            tokenSaver = object : Spotify.TokenSaver() {
                override suspend fun save(token: Spotify.Token) {
                    withContext(Dispatchers.IO) { spotifyTokensFile.writeText(Json.encodeToString(token)) }
                }
                override suspend fun load(): Spotify.Token? {
                    return withContext(Dispatchers.IO) { Json.decodeFromString(spotifyTokensFile.readText()) }
                }
                override suspend fun clear() {
                    withContext(Dispatchers.IO) { spotifyTokensFile.delete() }
                }
            },
            getCredentials = {
                withContext(Dispatchers.IO) {
                    Json.decodeFromString<Spotify.Credentials>(spotifyCredentialsFile.readText())
                }
            }
        )

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
                route("/Music") {
                    route("tracks") {
                        handle {
                            call.respondText("")
                        }
                        get {
                            call.respondText("sldkfj")
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
        runBlocking {
            logger.info { "--> Application::stop" }
            server.stopSuspend(1000, 1000)
            importWorker.stop()
            ytDlp.close()
            spotify.close()
            database.close()
            logger.info { "<-- Application::stop" }
        }
    }
}
