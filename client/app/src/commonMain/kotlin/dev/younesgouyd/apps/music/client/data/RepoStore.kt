package dev.younesgouyd.apps.music.client.data

import dev.younesgouyd.apps.music.client.ImportService
import dev.younesgouyd.apps.music.client.MediaController
import dev.younesgouyd.apps.music.client.data.repoes.*
import dev.younesgouyd.apps.music.client.data.room.AppDatabase
import dev.younesgouyd.apps.music.client.usecases.*
import dev.younesgouyd.libs.music.spotifyapi.SpotifyApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import java.io.File

class RepoStore(
    private val appDir: File,
    private val dbDir: File,
    private val applicationScope: CoroutineScope,
    private val database: AppDatabase,
    private val mediaPlayer: MediaController.MediaPlayer
) {
    lateinit var fileManager: FileManager private set
    lateinit var server: Server private set
    lateinit var spotifyApi: SpotifyApi private set
    lateinit var mediaController: MediaController
    private lateinit var importService: ImportService

    lateinit var clearImportItemUseCase: ClearImportItemUseCase private set
    lateinit var deleteFolderUseCase: DeleteFolderUseCase private set
    lateinit var exportUseCaseImpl: ExportUseCaseImpl
    lateinit var setTrackMetadataFromSpotifyUseCase: SetTrackMetadataFromSpotifyUseCase private set
    lateinit var unsetSpotifyTrackUseCase: UnsetSpotifyTrackUseCase private set

    lateinit var folderRepo: FolderRepo private set
    lateinit var importSessionItemRepo: ImportSessionItemRepo private set
    lateinit var importSessionRepo: ImportSessionRepo private set
    lateinit var mediaFileRepo: MediaFileRepo private set
    lateinit var playlistRepo: PlaylistRepo private set
    lateinit var playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo private set
    lateinit var settingsRepo: SettingsRepo private set
    lateinit var spotifyAlbumRepo: SpotifyAlbumRepo private set
    lateinit var spotifyArtistRepo: SpotifyArtistRepo private set
    lateinit var spotifyTrackRepo: SpotifyTrackRepo private set
    lateinit var tagRepo: TagRepo private set
    lateinit var tagTrackCrossRefRepo: TagTrackCrossRefRepo private set
    lateinit var trackRepo: TrackRepo private set

    suspend fun init() {
        println("--> RepoStore::init")
        fileManager = FileManager(
            appDir = appDir,
            dbDir = dbDir
        )
        settingsRepo = SettingsRepo(database.settingDao())
        folderRepo = FolderRepo(database.folderDao())
        playlistRepo = PlaylistRepo(database.playlistDao())
        playlistTrackCrossRefRepo = PlaylistTrackCrossRefRepo(database.playlistTrackCrossRefDao())
        trackRepo = TrackRepo(database.trackDao())
        mediaFileRepo = MediaFileRepo(
            dao = database.mediaFileDao(),
            fileManager = fileManager
        )
        importSessionRepo = ImportSessionRepo(database.importSessionDao())
        importSessionItemRepo = ImportSessionItemRepo(database.importSessionItemDao())
        tagRepo = TagRepo(database.tagDao())
        tagTrackCrossRefRepo = TagTrackCrossRefRepo(database.tagTrackCrossRefDao())

        spotifyAlbumRepo = SpotifyAlbumRepo(database.spotifyAlbumDao())
        spotifyArtistRepo = SpotifyArtistRepo(database.spotifyArtistDao())
        spotifyTrackRepo = SpotifyTrackRepo(database.spotifyTrackDao())

        settingsRepo.init()

        server = Server(
            serverAddress = settingsRepo.getServerAddress().map { it.value }.stateIn(applicationScope),
        )
        spotifyApi = SpotifyApi(
            tokenSaver = object : SpotifyApi.TokenSaver() {
                override suspend fun save(token: SpotifyApi.Token) {
                    settingsRepo.updateSpotifyToken(
                        accessToken = token.accessToken,
                        expirationTimeSeconds = token.expiresIn.toString(),
                        creationDatetimeEpochSecond = token.receivedAt.toString(),
                    )
                }
                override suspend fun load(): SpotifyApi.Token? {
                    val repo = settingsRepo
                    val accessToken = repo.getSpotifyAccessToken().first().value
                    if (accessToken.isBlank()) return null
                    val expiresIn = repo.getSpotifyTokenExpirationTime().first().value
                    if (expiresIn.isBlank()) return null
                    val receivedAt = repo.getSpotifyTokenDatetime().first().value
                    if (receivedAt.isBlank()) return null
                    return SpotifyApi.Token(
                        accessToken = accessToken,
                        expiresIn = expiresIn.toIntOrNull()!!,
                        receivedAt = receivedAt.toLongOrNull()!!,
                    )
                }

                override suspend fun clear() {
                    settingsRepo.updateSpotifyToken("", "", "")
                }
            },
            getCredentials = {
                SpotifyApi.Credentials(
                    clientId = settingsRepo.getSpotifyClientId().first().value,
                    clientSecret = settingsRepo.getSpotifyClientSecret().first().value
                )
            }
        )

        mediaController = MediaController(
            mediaPlayer = mediaPlayer,
            mediaFileRepo = mediaFileRepo,
            trackRepo = trackRepo,
            artistRepo = spotifyArtistRepo,
            albumRepo = spotifyAlbumRepo,
        )

        importService = ImportService(
            importSessionRepo = importSessionRepo,
            importSessionItemRepo = importSessionItemRepo,
            transaction = database.importTrx(),
            server = server,
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
            spotifyApi = spotifyApi,
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
        exportUseCaseImpl = ExportUseCaseImpl(
            dbDir = fileManager.dbDir,
            inspectionDir = fileManager.inspectionDir,
            mediaDir = fileManager.mediaDir
        )

        importService.start()
        println("<-- RepoStore::init")
    }

    fun release() {
        mediaController.release()
        spotifyApi.close()
        server.close()
        runBlocking {
            importService.stop()
        }
    }
}
