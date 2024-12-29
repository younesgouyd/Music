package dev.younesgouyd.apps.music.app.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mpatric.mp3agic.Mp3File
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
//            val parent: Long = folderRepo.add(folder.name, parent)
            println("folder: (URI:${folder.absoluteFile.toURI()}) (URL: ${folder.absoluteFile.toURI().toURL()})")
            for (file in folder.listFiles()!!) {
                if (file.isDirectory) {
                    recur(file, parent)
                } else {
                    println("file: (URL: ${file.absoluteFile.toURI().toURL()})")
                    if (file.extension.lowercase() == "mp3") {
                        val mp3file = Mp3File(file)
                        println("Length of this mp3 is: " + mp3file.lengthInSeconds + " seconds")
                        println("Bitrate: " + mp3file.bitrate + " kbps " + (if (mp3file.isVbr) "(VBR)" else "(CBR)"))
                        println("Sample rate: " + mp3file.sampleRate + " Hz")
                        if (mp3file.hasId3v1Tag()) {
                            println("Has ID3v1 tag")
                            val id3 = mp3file.id3v1Tag
                            println("Title: ${id3.title}")
                            println("Track: ${id3.track}")
                            println("Artist: ${id3.artist}")
                            println("Album: ${id3.album}")
                            println("Year: ${id3.year}")
                            println("Genre: ${id3.genre} (${id3.genreDescription})")
                            println("Comment: ${id3.comment}")
                        }
                        if (mp3file.hasId3v2Tag()) {
                            println("Has ID3v2 tag")
                            val id3 = mp3file.id3v2Tag
                            println("Track: " + id3.track)
                            println("Artist: " + id3.artist)
                            println("Title: " + id3.title)
                            println("Album: " + id3.album)
                            println("Year: " + id3.year)
                            println("Genre: " + id3.genre + " (" + id3.genreDescription + ")")
                            println("Comment: " + id3.comment)
                            println("Lyrics: " + id3.lyrics)
                            println("Composer: " + id3.composer)
                            println("Publisher: " + id3.publisher)
                            println("Original artist: " + id3.originalArtist)
                            println("Album artist: " + id3.albumArtist)
                            println("Copyright: " + id3.copyright)
                            println("URL: " + id3.url)
                            println("Encoder: " + id3.encoder)
                            val albumImageData = id3.albumImage
                            if (albumImageData != null) {
                                println("Have album image data, length: " + albumImageData.size + " bytes")
                                println("Album image mime type: " + id3.albumImageMimeType)
                            }
                        }
                    }
                    println("=======================================================================================")
//                    trackRepo.add(
//                        name = file.name,
//                        folderId = parent,
//                        albumId = null,
//                        audioUrl = file.toURI().toString(),
//                        videoUrl = null
//                    )
                }
            }
        }

        val main = File("/home/neo/Downloads/Hip-Hop & Rap")
        if (main.isDirectory) recur(main, null)
    }
}
