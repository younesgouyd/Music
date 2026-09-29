package dev.younesgouyd.apps.music.server.multiplatform

import dev.younesgouyd.apps.music.common.*
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.server.multiplatform.data.FileManager
import dev.younesgouyd.apps.music.server.multiplatform.data.RepoStore
import dev.younesgouyd.apps.music.server.multiplatform.data.room.AppDatabase
import dev.younesgouyd.apps.music.server.multiplatform.spotify.Spotify
import dev.younesgouyd.apps.music.server.multiplatform.usecases.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.network.tls.certificates.*
import io.ktor.network.tls.extensions.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.event.Level
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.security.auth.x500.X500Principal
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class Application {
    val state get() = _state.asStateFlow()
    val certificateFingerprint get() = _certificateFingerprint.asStateFlow()
    val password get() = _password.asStateFlow()

    private val logger = KotlinLogging.logger {}
    private lateinit var database: AppDatabase
    private lateinit var fileManager: FileManager
    private lateinit var clientManager: ClientManager
    private lateinit var repoStore: RepoStore
    private lateinit var mediaController: MediaController
    private lateinit var ytDlp: YtDlp
    private lateinit var spotify: Spotify

    private lateinit var importWorker: ImportWorker

    private lateinit var unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase
    private lateinit var setTrackMetadataFromSpotifyUseCase: SetTrackMetadataFromSpotifyUseCase
    private lateinit var clearImportItemUseCase: ClearImportItemUseCase
    private lateinit var deleteFolderUseCase: DeleteFolderUseCase
    private lateinit var prepareImportUseCase: PrepareImportUseCase

    private lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

    private val _state: MutableStateFlow<ServerState> = MutableStateFlow(ServerState.Stopped)
    private val _certificateFingerprint: MutableStateFlow<String> = MutableStateFlow("")
    private val _password: MutableStateFlow<String> = MutableStateFlow("")

    fun start(
        homeDir: File,
        ytDlpServerInfo: YtDlpServerInfo
    ) {
        if (_state.value == ServerState.Starting || _state.value == ServerState.Stopping) return
        logger.info { "--> Application::start" }
        _state.value = ServerState.Starting
        val appDir = File(homeDir, "younesmusicserverdata").also { it.mkdir() }
        val mediaDir = File(appDir, "media").also { it.mkdir() }
        // logs dir is set in logback.xml
        val dbDir = File(appDir, "database").also { it.mkdir() }
        val dbFile = File(dbDir, "younesmusic.db").also { if (!it.exists()) it.createNewFile() }
        val keystoreFile = File(appDir, "keystore.p12")
        val spotifyDir = File(appDir, "spotify").also { it.mkdir() }
        val spotifyTokensFile = File(spotifyDir, "tokens.json").also { if (!it.exists()) it.createNewFile() }
        val spotifyCredentialsFile = File(spotifyDir, "credentials.json").also { if (!it.exists()) it.createNewFile() }

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

        clientManager = ClientManager()
        repoStore = RepoStore(database)
        mediaController = MediaController(
            mediaFileRepo = repoStore.mediaFileRepo,
            trackRepo = repoStore.trackRepo,
            artistRepo = repoStore.spotifyArtistRepo,
            albumRepo = repoStore.spotifyAlbumRepo,
            clientManager = clientManager
        )

        ytDlp = YtDlp(
            serverHost = ytDlpServerInfo.ipAddress,
            serverFingerprint = ytDlpServerInfo.serverFingerprint,
            password = ytDlpServerInfo.password
        )

        spotify = Spotify(tokensFile = spotifyTokensFile, credentialsFile = spotifyCredentialsFile)

        importWorker = ImportWorker(
            importSessionRepo = repoStore.importSessionRepo,
            importSessionItemRepo = repoStore.importSessionItemRepo,
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
            trackRepo = repoStore.trackRepo,
            spotifyAlbumRepo = repoStore.spotifyAlbumRepo,
            transaction = database.setTrackMetadataFromSpotify(),
            spotify = spotify,
            fileManager = fileManager
        )
        clearImportItemUseCase = ClearImportItemUseCase(
            unsetSpotifyTrackUseCase = unsetSpotifyTrackUseCase,
            trackRepo = repoStore.trackRepo,
            transaction = database.clearImportSessionItem(),
            fileManager = fileManager
        )
        deleteFolderUseCase = DeleteFolderUseCase(
            playlistRepo = repoStore.playlistRepo,
            trackRepo = repoStore.trackRepo,
            clearImportItemUseCase = clearImportItemUseCase,
            folderRepo = repoStore.folderRepo
        )
        prepareImportUseCase = PrepareImportUseCase(
            fileManager = fileManager,
            transaction = database.prepareImportFromInternet()
        )

        _password.value = generatePassword()
        server = embeddedServer(
            factory = Netty,
            configure = {
                val alias = "blabla"
                val keyStorePassword = "123456789"
                val passwordChars = keyStorePassword.toCharArray()
                val keyStore: KeyStore = run {
                    var loaded: KeyStore? = null
                    if (keystoreFile.exists() && keystoreFile.length() > 0) {
                        val loadedKeyStore = KeyStore.getInstance("PKCS12")
                        keystoreFile.inputStream().use { input ->
                            loadedKeyStore.load(input, passwordChars)
                        }
                        loaded = loadedKeyStore
                    }
                    val valid: Boolean = loaded?.let { loaded ->
                        try {
                            val cert = loaded.getCertificate(alias) as? X509Certificate
                            cert?.checkValidity()
                            cert != null
                        } catch (_: Exception) {
                            false
                        }
                    } ?: false
                    if (valid) {
                        loaded
                    } else {
                        val pkcs12KeyStore: KeyStore = run {
                            val tempKeyStore: KeyStore = buildKeyStore {
                                certificate(alias) {
                                    hash = HashAlgorithm.SHA256
                                    sign = SignatureAlgorithm.RSA
                                    password = keyStorePassword
                                    subject = X500Principal("CN=localhost, O=Music, C=MA")
                                    daysValid = 3
                                    keySizeInBits = 2048
                                    keyType = KeyType.Server
                                }
                            }
                            KeyStore.getInstance("PKCS12").apply {
                                load(null, passwordChars)
                                setKeyEntry(
                                    /* alias = */ alias,
                                    /* key = */ tempKeyStore.getKey(alias, passwordChars) as PrivateKey,
                                    /* password = */ passwordChars,
                                    /* chain = */ tempKeyStore.getCertificateChain(alias)
                                )
                            }
                        }
                        keystoreFile.outputStream().use { output ->
                            pkcs12KeyStore.store(output, passwordChars)
                        }
                        pkcs12KeyStore
                    }
                }
                _certificateFingerprint.value = run {
                    val cert = keyStore.getCertificate(alias) as? X509Certificate ?: error("Certificate with alias '$alias' not found in KeyStore.")
                    cert.sha256Hash()
                }
                sslConnector(
                    keyStore = keyStore,
                    keyAlias = alias,
                    keyStorePassword = { keyStorePassword.toCharArray() },
                    privateKeyPassword = { keyStorePassword.toCharArray() }
                ) {
                    port = 8443
                    enabledProtocols = listOf("TLSv1.3")
                }
            }
        ) {
            install(CallLogging) {
                level = Level.TRACE
                format { call ->
                    val status = call.response.status()
                    val httpMethod = call.request.httpMethod.value
                    val userAgent = call.request.headers["User-Agent"]
                    buildString {
                        appendLine()
                        appendLine("=================================================")
                        appendLine("$httpMethod ${call.request.path()}")
                        appendLine("Status: $status, User agent: $userAgent")
                        appendLine("request headers: ")
                        call.request.headers.forEach { name, values ->
                            appendLine("  $name: ${values.joinToString(", ")}")
                        }
                        appendLine("=================================================")
                    }
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(json)
                pingPeriod = 10.seconds
                timeout = 10.seconds
            }
            install(Authentication) {
                provider("custom-auth") {
                    authenticate { context ->
                        if (!context.call.verifyPassword(_password.value)) {
                            context.call.respond(HttpStatusCode.Unauthorized, "Invalid or missing credentials")
                        }
                    }
                }
            }
            routing {
                authenticate("custom-auth") {
                    route("/") {
                        handle {
                            call.respond("music server")
                        }
                    }
                    webSocket("rpc") {
                        val sessionId = Uuid.random()
                        try {
                            val client = Client(
                                id = sessionId,
                                session = this,
                                repoStore = repoStore,
                                mediaController = mediaController,
                                ytDlp = ytDlp,
                                spotify = spotify,
                                unsetSpotifyTrackUseCase = unsetSpotifyTrackUseCase,
                                setTrackMetadataFromSpotifyUseCase = setTrackMetadataFromSpotifyUseCase,
                                clearImportItemUseCase = clearImportItemUseCase,
                                deleteFolderUseCase = deleteFolderUseCase,
                                prepareImportUseCase = prepareImportUseCase,
                            )
                            clientManager.register(client)
                            client.connect()
                        } finally {
                            clientManager.remove(sessionId)
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
        }

        server.start()
        importWorker.start()
        _state.value = ServerState.Running
        logger.info { "<-- Application::start" }
    }

    suspend fun stop() {
        if (_state.value == ServerState.Stopping || _state.value == ServerState.Stopped) return
        logger.info { "--> Application::stop" }
        withContext(Dispatchers.IO) {
            _state.value = ServerState.Stopping
            try { mediaController.release() } catch (e: Throwable) { logger.error(e) {} }
            try { server.stopSuspend(1000, 1000) } catch (e: Throwable) { logger.error(e) {} }
            try { importWorker.stop() } catch (e: Throwable) { logger.error(e) {} }
            try { ytDlp.close() } catch (e: Throwable) { logger.error(e) {} }
            try { spotify.close() } catch (e: Throwable) { logger.error(e) {} }
            try { database.close() } catch (e: Throwable) { logger.error(e) {} }
            _state.value = ServerState.Stopped
        }
        logger.info { "<-- Application::stop" }
    }
}
