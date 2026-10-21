package dev.younesgouyd.apps.music.server.multiplatform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.younesgouyd.apps.music.common.generatePassword
import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.ClientId
import dev.younesgouyd.apps.music.common.models.MediaFileId
import dev.younesgouyd.apps.music.common.models.WSMessage
import dev.younesgouyd.apps.music.common.sha256Hash
import dev.younesgouyd.apps.music.common.ui.Component
import dev.younesgouyd.apps.music.common.verifyPassword
import dev.younesgouyd.apps.music.server.multiplatform.data.FileManager
import dev.younesgouyd.apps.music.server.multiplatform.data.RepoStore
import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.SettingRepo
import dev.younesgouyd.apps.music.server.multiplatform.data.room.AppDatabase
import dev.younesgouyd.apps.music.server.multiplatform.spotify.Spotify
import dev.younesgouyd.apps.music.server.multiplatform.spotify.Spotify.Credentials
import dev.younesgouyd.apps.music.server.multiplatform.spotify.Spotify.Token
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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import javax.security.auth.x500.X500Principal
import kotlin.time.Duration.Companion.seconds

abstract class Application(appDir: File) {
    companion object {
        protected const val APP_DIR_NAME = "younesmusicserverdata"
    }

    private val logger = KotlinLogging.logger {}
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val currentComponent: MutableStateFlow<Component> = MutableStateFlow(
        object : Component() {
            override val title: String get() = ""

            @Composable
            override fun show(modifier: Modifier) {
                AppThemeContainer {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Please wait...")
                    }
                }
            }

            override fun clear() {
                coroutineScope.cancel()
            }
        }
    )

    init {
        coroutineScope.launch {
            val mediaDir = File(appDir, "media")
            // logs dir is set in logback.xml
            val dbDir = File(appDir, "database")
            val dbFile = File(dbDir, "younesmusic.db")
            val keystoreFile = File(appDir, "keystore.p12")
            withContext(Dispatchers.IO) {
                mediaDir.mkdir()
                // logs dir is set in logback.xml
                dbDir.mkdir()
                if (!dbFile.exists()) dbFile.createNewFile()
                if (!keystoreFile.exists()) keystoreFile.createNewFile()
            }
            val database = createDatabaseInstance(dbFile)
            val fileManager = FileManager(mediaDir)
            val repoStore = RepoStore(database)
            repoStore.clientSessionRepo.updateDisconnectionFlag()
            val webSocketSessionHandlersMap = ConcurrentHashMap<ClientId, WebSocketSessionHandler>()
            val websocketSessionHandlers = WebSocketSessionHandlers(webSocketSessionHandlersMap)
            val mediaController = MediaController(
                clientRepo = repoStore.clientRepo,
                websocketSessionHandlers = websocketSessionHandlers,
                mediaFileRepo = repoStore.mediaFileRepo,
                trackRepo = repoStore.trackRepo,
                artistRepo = repoStore.spotifyArtistRepo,
                albumRepo = repoStore.spotifyAlbumRepo
            )
            val ytDlp = YtDlp(repoStore.settingRepo)
            val spotify = Spotify(
                credentialsSaver = object : Spotify.CredentialsSaver() {
                    val settingName = "spotify_credentials"
                    override suspend fun save(credentials: Credentials) {
                        repoStore.settingRepo.set(settingName, Json.encodeToString(credentials))
                    }
                    override suspend fun load(): Credentials? {
                        val setting = repoStore.settingRepo.get(settingName).firstOrNull() ?: return null
                        return Json.decodeFromString<Credentials>(setting.value)
                    }
                    override suspend fun clear() {
                        repoStore.settingRepo.delete(settingName)
                    }
                },
                tokenSaver = object : Spotify.TokenSaver() {
                    private val settingName = "spotify_token"
                    override suspend fun save(token: Token) {
                        repoStore.settingRepo.set(settingName, Json.encodeToString(token))
                    }
                    override suspend fun load(): Token? {
                        val setting = repoStore.settingRepo.get(settingName).firstOrNull() ?: return null
                        return Json.decodeFromString<Token>(setting.value)
                    }
                    override suspend fun clear() {
                        repoStore.settingRepo.delete(settingName)
                    }
                }
            )
            val importWorker = ImportWorker(
                importSessionRepo = repoStore.importSessionRepo,
                importSessionItemRepo = repoStore.importSessionItemRepo,
                transaction = database.importTrx(),
                ytDlp = ytDlp,
                fileManager = fileManager,
            )

            val unsetSpotifyTrackUseCase = UnsetSpotifyTrackUseCase(
                transaction = database.unsetSpotifyTrack(),
                fileManager = fileManager
            )
            val setTrackMetadataFromSpotifyUseCase = SetTrackMetadataFromSpotifyUseCase(
                unsetSpotifyTrackUseCase = unsetSpotifyTrackUseCase,
                trackRepo = repoStore.trackRepo,
                spotifyAlbumRepo = repoStore.spotifyAlbumRepo,
                transaction = database.setTrackMetadataFromSpotify(),
                spotify = spotify,
                fileManager = fileManager
            )
            val clearImportItemUseCase = ClearImportItemUseCase(
                unsetSpotifyTrackUseCase = unsetSpotifyTrackUseCase,
                trackRepo = repoStore.trackRepo,
                transaction = database.clearImportSessionItem(),
                fileManager = fileManager
            )
            val deleteFolderUseCase = DeleteFolderUseCase(
                playlistRepo = repoStore.playlistRepo,
                trackRepo = repoStore.trackRepo,
                clearImportItemUseCase = clearImportItemUseCase,
                folderRepo = repoStore.folderRepo
            )
            val prepareImportUseCase = PrepareImportUseCase(
                fileManager = fileManager,
                transaction = database.prepareImportFromInternet()
            )

            // keystore, certificate fingerprint, password
            val alias = "blabla"
            val keyStorePassword = "123456789"
            val keyStorePasswordChars = keyStorePassword.toCharArray()
            val (keyStore: KeyStore, password: String, certificateFingerprint: String) = run {
                var loaded: KeyStore? = null
                if (keystoreFile.exists() && keystoreFile.length() > 0) {
                    val loadedKeyStore = KeyStore.getInstance("PKCS12")
                    keystoreFile.inputStream().use { input ->
                        loadedKeyStore.load(input, keyStorePasswordChars)
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
                logger.debug { "valid: $valid" }
                if (valid) {
                    val password = repoStore.settingRepo.get(SettingRepo.SettingName.PASSWORD).first()?.value.let {
                        if (it.isNullOrBlank()) {
                            val pass = generatePassword()
                            repoStore.settingRepo.set(SettingRepo.SettingName.PASSWORD, pass)
                            pass
                        } else {
                            it
                        }
                    }
                    Triple(
                        loaded,
                        password,
                        (loaded.getCertificate(alias) as X509Certificate).sha256Hash()
                    )
                } else {
                    val pkcs12KeyStore: KeyStore = run {
                        val tempKeyStore: KeyStore = buildKeyStore {
                            certificate(alias) {
                                hash = HashAlgorithm.SHA256
                                sign = SignatureAlgorithm.RSA
                                this.password = keyStorePassword // TODO
                                subject = X500Principal("CN=localhost, O=Music, C=MA")
                                daysValid = 3
                                keySizeInBits = 2048
                                keyType = KeyType.Server
                            }
                        }
                        KeyStore.getInstance("PKCS12").apply {
                            load(null, keyStorePasswordChars)
                            setKeyEntry(
                                /* alias = */ alias,
                                /* key = */ tempKeyStore.getKey(alias, keyStorePasswordChars) as PrivateKey,
                                /* password = */ keyStorePasswordChars,
                                /* chain = */ tempKeyStore.getCertificateChain(alias)
                            )
                        }
                    }
                    keystoreFile.outputStream().use { output ->
                        pkcs12KeyStore.store(output, keyStorePasswordChars)
                    }
                    Triple(
                        pkcs12KeyStore,
                        generatePassword().also {
                            repoStore.settingRepo.set(SettingRepo.SettingName.PASSWORD, it)
                        },
                        (pkcs12KeyStore.getCertificate(alias) as X509Certificate).sha256Hash()
                    )
                }
            }
            // -----------------------------------------------

            val serverDiscoveryService = ServerDiscoveryService()
            val server = embeddedServer(
                factory = Netty,
                configure = {
                    sslConnector(
                        keyStore = keyStore,
                        keyAlias = alias,
                        keyStorePassword = { keyStorePasswordChars },
                        privateKeyPassword = { keyStorePasswordChars }
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
                            if (!context.call.verifyPassword(password)) {
                                context.call.respond(HttpStatusCode.Unauthorized, "Invalid or missing credentials")
                            }
                        }
                    }
                }
                routing {
                    authenticate("custom-auth") {
                        val mutex = Mutex()
                        webSocket("rpc") {
                            val connectionDatetime = System.currentTimeMillis()
                            val clientRepo = repoStore.clientRepo
                            val sessionRepo = repoStore.clientSessionRepo
                            val clientId: ClientId = run {
                                val header = call.request.headers["Client-ID"]
                                if (header != null) {
                                    try {
                                        ClientId(header.toLong())
                                    } catch (_: Exception) {
                                        logger.error { "received invalid client id" }
                                        call.respond(HttpStatusCode.BadRequest, "invalid client id")
                                        return@webSocket
                                    }
                                } else {
                                    val id = clientRepo.add("", null)
                                    sendSerialized<WSMessage>(WSMessage.Registration(id))
                                    id
                                }
                            }
                            val (sessionId, handler) = mutex.withLock {
                                val isConnected = clientRepo.getConnected().first().any { it.id == clientId }
                                if (isConnected) {
                                    logger.error { "client $clientId is already connected" }
                                    call.respond(HttpStatusCode.BadRequest, "already connected")
                                    return@webSocket
                                }
                                val sessionId = sessionRepo.add(clientId, connectionDatetime)
                                val handler = WebSocketSessionHandler(
                                    clientId = clientId,
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
                                webSocketSessionHandlersMap[clientId] = handler
                                sessionId to handler
                            }
                            try {
                                handler.run()
                            } finally {
                                val disconnectionDatetime = System.currentTimeMillis()
                                webSocketSessionHandlersMap.remove(clientId, handler)
                                handler.close()
                                sessionRepo.update(
                                    id = sessionId,
                                    disconnectionDatetime = disconnectionDatetime
                                )
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
            serverDiscoveryService.start()

            currentComponent.value = MainUi(
                password = password,
                certificateFingerprint = certificateFingerprint,
                clientRepo = repoStore.clientRepo,
                clientSessionRepo = repoStore.clientSessionRepo,
                settingRepo = repoStore.settingRepo
            )
        }
    }

    @Composable
    fun show(modifier: Modifier) {
        val currentComponent by currentComponent.collectAsState()

        currentComponent.show(modifier.fillMaxSize())
    }

    protected abstract fun createDatabaseInstance(dbFile: File): AppDatabase
}
