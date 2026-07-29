package dev.younesgouyd.apps.music.client.common.data

import dev.younesgouyd.apps.music.client.common.data.repoes.*
import dev.younesgouyd.apps.music.common.json
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking

class RepoStore {
    /* TODO: private */ val client = HttpClient(CIO) {
        install(Logging) { level = LogLevel.ALL }
        install(ContentNegotiation) { json(json) }
    }

    val folderRepo = FolderRepo(client)
    val importSessionItemRepo = ImportSessionItemRepo(client)
    val importSessionRepo = ImportSessionRepo(client)
    val inspectionRepo = InspectionRepo(client)
    val mediaFileRepo = MediaFileRepo(client)
    val playlistRepo = PlaylistRepo(client)
    val playlistTrackCrossRefRepo = PlaylistTrackCrossRefRepo(client)
    val settingsRepo = SettingsRepo(client)
    val spotifyAlbumRepo = SpotifyAlbumRepo(client)
    val spotifyArtistRepo = SpotifyArtistRepo(client)
    val spotifyAuthRepo = SpotifyAuthRepo(client)
    val spotifySearchRepo = SpotifySearchRepo(client)
    val spotifyTrackRepo = SpotifyTrackRepo(client)
    val tagRepo = TagRepo(client)
    val tagTrackCrossRefRepo = TagTrackCrossRefRepo(client)
    val trackRepo = TrackRepo(client)

    fun release() {
        runBlocking {
            client.close()
            client.coroutineContext.job.join()
        }
    }
}
