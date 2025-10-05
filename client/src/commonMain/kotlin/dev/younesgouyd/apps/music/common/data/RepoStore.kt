package dev.younesgouyd.apps.music.common.data

import dev.younesgouyd.apps.music.common.data.repoes.*
import dev.younesgouyd.apps.music.common.data.room.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RepoStore(
    private val applicationScope: CoroutineScope,
    private val database: AppDatabase
) {
    lateinit var server: Server private set
    lateinit var settingsRepo: SettingsRepo private set
    lateinit var albumRepo: AlbumRepo private set
    lateinit var artistRepo: ArtistRepo private set
    lateinit var artistTrackCrossRefRepo: ArtistTrackCrossRefRepo private set
    lateinit var folderRepo: FolderRepo private set
    lateinit var playlistRepo: PlaylistRepo private set
    lateinit var playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo private set
    lateinit var trackRepo: TrackRepo private set
    lateinit var mediaFileRepo: MediaFileRepo private set
    lateinit var importSessionRepo: ImportSessionRepo private set

    suspend fun init() {
        settingsRepo = SettingsRepo(database.settingDao())
        folderRepo = FolderRepo(database.folderDao())
        albumRepo = AlbumRepo(database.albumDao())
        artistRepo = ArtistRepo(database.artistDao())
        artistTrackCrossRefRepo = ArtistTrackCrossRefRepo(database.artistTrackCrossRefDao())
        playlistRepo = PlaylistRepo(database.playlistDao())
        playlistTrackCrossRefRepo = PlaylistTrackCrossRefRepo(database.playlistTrackCrossRefDao())
        trackRepo = TrackRepo(database.trackDao())
        mediaFileRepo = MediaFileRepo(database.mediaFileDao())
        importSessionRepo = ImportSessionRepo(database.importSessionDao())

        settingsRepo.init()

        server = Server(settingsRepo.getServerAddress().map { it!!.value }.stateIn(applicationScope))
    }
}
