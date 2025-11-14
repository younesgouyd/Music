package dev.younesgouyd.apps.music.client.data

import dev.younesgouyd.apps.music.client.data.repoes.*
import dev.younesgouyd.apps.music.client.data.room.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File

class RepoStore(
    private val appDir: File,
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
    lateinit var importSessionWithItemsRepo: ImportSessionWithItemsRepo

    suspend fun init() {
        fileManager = FileManager(appDir)
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

        settingsRepo.init()

        server = Server(
            serverAddress = settingsRepo.getServerAddress().map { it!!.value }.stateIn(applicationScope),
        )
    }
}
