package dev.younesgouyd.apps.music.common.data

import app.cash.sqldelight.db.SqlDriver
import dev.younesgouyd.apps.music.common.data.repoes.*
import dev.younesgouyd.apps.music.common.data.sqldelight.YounesMusic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RepoStore(
    private val applicationScope: CoroutineScope,
    private val dbDriver: SqlDriver
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
        val database = YounesMusic(dbDriver)
        settingsRepo = SettingsRepo(queries = database.settingQueries)
        folderRepo = FolderRepo(database.folderQueries)
        albumRepo = AlbumRepo(database.albumQueries)
        artistRepo = ArtistRepo(database.artistQueries)
        artistTrackCrossRefRepo = ArtistTrackCrossRefRepo(database.artistTrackCrossRefQueries)
        playlistRepo = PlaylistRepo(database.playlistQueries)
        playlistTrackCrossRefRepo = PlaylistTrackCrossRefRepo(database.playlistTrackCrossRefQueries)
        trackRepo = TrackRepo(database.trackQueries)
        mediaFileRepo = MediaFileRepo(database.mediaFileQueries)
        importSessionRepo = ImportSessionRepo(
            queries = database.importSessionQueries,
            itemQueries = database.importSessionItemQueries
        )

        settingsRepo.init()

        server = Server(settingsRepo.getServerAddress().map { it?.value_ }.stateIn(applicationScope))
    }
}
