package dev.younesgouyd.apps.music.app.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.younesgouyd.apps.music.app.data.repoes.*
import dev.younesgouyd.apps.music.app.data.sqldelight.YounesMusic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class RepoStore {
    private lateinit var database: YounesMusic

    lateinit var settingsRepo: SettingsRepo private set
    lateinit var albumRepo: AlbumRepo private set
    lateinit var artistRepo: ArtistRepo private set
    lateinit var artistTrackCrossRefRepo: ArtistTrackCrossRefRepo private set
    lateinit var folderRepo: FolderRepo private set
    lateinit var playlistRepo: PlaylistRepo private set
    lateinit var playlistTrackCrossRefRepo: PlaylistTrackCrossRefRepo private set
    lateinit var trackRepo: TrackRepo private set

    suspend fun init() {
        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:younesmusic.db",
            properties = Properties().apply { put("foreign_keys", "true") }
        )
        val file = File("younesmusic.db")
        if (!file.exists()) {
            withContext(Dispatchers.IO) {
                file.createNewFile()
                YounesMusic.Schema.create(driver)
            }
        }
        database = YounesMusic(driver)

        settingsRepo = SettingsRepo()
        folderRepo = FolderRepo(database.folderQueries)
        albumRepo = AlbumRepo(database.albumQueries)
        artistRepo = ArtistRepo(database.artistQueries)
        artistTrackCrossRefRepo = ArtistTrackCrossRefRepo(database.artistTrackCrossRefQueries)
        playlistRepo = PlaylistRepo(database.playlistQueries)
        playlistTrackCrossRefRepo = PlaylistTrackCrossRefRepo(database.playlistTrackCrossRefQueries)
        trackRepo = TrackRepo(database.trackQueries)

        settingsRepo.init()

//        genData()
    }

    private suspend fun genData() {
        suspend fun recur(folder: File, parent: Long?) {
            val parent: Long = folderRepo.add(folder.name, parent)
            println("folder: (URI:${folder.absoluteFile.toURI()}) (URL: ${folder.absoluteFile.toURI().toURL()})")
            for (file in folder.listFiles()!!) {
                if (file.isDirectory) {
                    recur(file, parent)
                } else {
                    println("file: (URI:${file.absoluteFile.toURI()}) (URL: ${file.absoluteFile.toURI().toURL()})")
                    trackRepo.add(
                        name = file.name,
                        folderId = parent,
                        albumId = null,
                        audioUrl = file.toURI().toString(),
                        videoUrl = null
                    )
                }
            }
        }

        val main = File("/home/neo/Downloads/Hip-Hop & Rap")
        if (main.isDirectory) recur(main, null)
    }
}
