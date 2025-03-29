package dev.younesgouyd.apps.music.common.data

import dev.younesgouyd.apps.music.common.data.repoes.*
import dev.younesgouyd.apps.music.common.data.sqldelight.YounesMusic
import dev.younesgouyd.apps.music.common.util.FileManager

class RepoStore(
    private val database: YounesMusic,
    private val fileManager: FileManager
) {
    lateinit var settingsRepo: SettingsRepo private set
    lateinit var albumRepo: AlbumRepo private set
    lateinit var artistRepo: ArtistRepo private set
    lateinit var artistTrackCrossRefRepo: ArtistTrackCrossRefRepo private set
    lateinit var folderRepo: FolderRepo private set
    lateinit var playlistRepo: PlaylistRepo private set
    lateinit var playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo private set
    lateinit var trackRepo: TrackRepo private set

    suspend fun init() {
        settingsRepo = SettingsRepo(fileManager)
        folderRepo = FolderRepo(database.folderQueries)
        albumRepo = AlbumRepo(database.albumQueries)
        artistRepo = ArtistRepo(database.artistQueries)
        artistTrackCrossRefRepo = ArtistTrackCrossRefRepo(database.artistTrackCrossRefQueries)
        playlistRepo = PlaylistRepo(database.playlistQueries)
        playlistTrackCrossRefRepo = PlaylistTrackCrossRefRepo(database.playlistTrackCrossRefQueries)
        trackRepo = TrackRepo(database.trackQueries)

        settingsRepo.init()
    }
}
