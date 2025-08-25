package dev.younesgouyd.apps.music.common.data

import app.cash.sqldelight.db.SqlDriver
import dev.younesgouyd.apps.music.common.data.repoes.*
import dev.younesgouyd.apps.music.common.data.sqldelight.YounesMusic
import dev.younesgouyd.apps.music.common.util.FileManager

class RepoStore(
    private val dbDriver: SqlDriver,
    dataDirectory: String
) {
    private val fileManager = FileManager(dataDirectory)
    lateinit var settingsRepo: SettingsRepo private set
    lateinit var albumRepo: AlbumRepo private set
    lateinit var artistRepo: ArtistRepo private set
    lateinit var artistTrackCrossRefRepo: ArtistTrackCrossRefRepo private set
    lateinit var folderRepo: FolderRepo private set
    lateinit var playlistRepo: PlaylistRepo private set
    lateinit var playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo private set
    lateinit var trackRepo: TrackRepo private set
    lateinit var mediaFileRepo: MediaFileRepo private set

    suspend fun init() {
        val database = YounesMusic(dbDriver)
        settingsRepo = SettingsRepo(fileManager)
        folderRepo = FolderRepo(database.folderQueries)
        albumRepo = AlbumRepo(database.albumQueries)
        artistRepo = ArtistRepo(database.artistQueries)
        artistTrackCrossRefRepo = ArtistTrackCrossRefRepo(database.artistTrackCrossRefQueries)
        playlistRepo = PlaylistRepo(database.playlistQueries)
        playlistTrackCrossRefRepo = PlaylistTrackCrossRefRepo(database.playlistTrackCrossRefQueries)
        trackRepo = TrackRepo(database.trackQueries)
        mediaFileRepo = MediaFileRepo(database.mediaFileQueries)

        settingsRepo.init()
    }
}
