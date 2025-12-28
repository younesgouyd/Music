package dev.younesgouyd.apps.music.client.data

import dev.younesgouyd.apps.music.client.data.repoes.*
import dev.younesgouyd.apps.music.client.data.room.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File

class RepoStore(
    private val appDir: File,
    private val dbDir: File,
    private val applicationScope: CoroutineScope,
    private val database: AppDatabase
) {
    lateinit var fileManager: FileManager private set
    lateinit var server: Server private set
    lateinit var settingsRepo: SettingsRepo private set
    lateinit var artistRepo: ArtistRepo private set
    lateinit var artistTrackCrossRefRepo: ArtistTrackCrossRefRepo private set
    lateinit var folderRepo: FolderRepo private set
    lateinit var playlistRepo: PlaylistRepo private set
    lateinit var playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo private set
    lateinit var trackRepo: TrackRepo private set
    lateinit var mediaFileRepo: MediaFileRepo private set
    lateinit var importSessionRepo: ImportSessionRepo private set
    lateinit var importSessionItemRepo: ImportSessionItemRepo private set
    lateinit var importSessionWithItemsRepo: ImportSessionWithItemsRepo private set
    lateinit var mediaFileTrackCrossRefRepo: MediaFileTrackCrossRefRepo private set
    lateinit var mediaFileImportSessionCrossRefRepo: MediaFileImportSessionCrossRefRepo private set
    lateinit var mediaFileImportSessionItemCrossRefRepo: MediaFileImportSessionItemCrossRefRepo private set
    lateinit var mediaFileArtistCrossRefRepo: MediaFileArtistCrossRefRepo private set
    lateinit var mediaFilePlaylistCrossRefRepo: MediaFilePlaylistCrossRefRepo private set
    lateinit var tagRepo: TagRepo private set
    lateinit var tagTrackCrossRefRepo: TagTrackCrossRefRepo private set
    lateinit var playlistTrackViewRepo: PlaylistTrackViewRepo private set

    suspend fun init() {
        println("--> RepoStore::init")
        fileManager = FileManager(
            appDir = appDir,
            dbDir = dbDir
        )
        settingsRepo = SettingsRepo(database.settingDao())
        folderRepo = FolderRepo(database.folderDao())
        artistRepo = ArtistRepo(database.artistDao())
        artistTrackCrossRefRepo = ArtistTrackCrossRefRepo(database.artistTrackCrossRefDao())
        playlistRepo = PlaylistRepo(database.playlistDao())
        playlistTrackCrossRefRepo = PlaylistTrackCrossRefRepo(database.playlistTrackCrossRefDao())
        trackRepo = TrackRepo(database.trackDao())
        mediaFileRepo = MediaFileRepo(
            dao = database.mediaFileDao(),
            fileManager = fileManager
        )
        importSessionRepo = ImportSessionRepo(database.importSessionDao())
        importSessionItemRepo = ImportSessionItemRepo(database.importSessionItemDao())
        importSessionWithItemsRepo = ImportSessionWithItemsRepo(
            dao = database.importSessionWithItemsDao(),
            fileManager = fileManager
        )
        mediaFileTrackCrossRefRepo = MediaFileTrackCrossRefRepo(database.mediaFileTrackCrossRefDao())
        mediaFileImportSessionCrossRefRepo = MediaFileImportSessionCrossRefRepo(database.mediaFileImportSessionCrossRefDao())
        mediaFileImportSessionItemCrossRefRepo = MediaFileImportSessionItemCrossRefRepo(database.mediaFileImportSessionItemCrossRefDao())
        mediaFileArtistCrossRefRepo = MediaFileArtistCrossRefRepo(database.mediaFileArtistCrossRefDao())
        mediaFilePlaylistCrossRefRepo = MediaFilePlaylistCrossRefRepo(database.mediaFilePlaylistCrossRefDao())
        tagRepo = TagRepo(database.tagDao())
        tagTrackCrossRefRepo = TagTrackCrossRefRepo(database.tagTrackCrossRefDao())

        settingsRepo.init()

        playlistTrackViewRepo = PlaylistTrackViewRepo(database.playlistTrackViewDao())

        server = Server(
            serverAddress = settingsRepo.getServerAddress().map { it!!.value }.stateIn(applicationScope),
        )
        println("<-- RepoStore::init")
    }
}
