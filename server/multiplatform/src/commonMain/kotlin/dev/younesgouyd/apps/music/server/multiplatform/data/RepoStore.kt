package dev.younesgouyd.apps.music.server.multiplatform.data

import dev.younesgouyd.apps.music.server.multiplatform.data.repoes.*
import dev.younesgouyd.apps.music.server.multiplatform.data.room.AppDatabase

class RepoStore(database: AppDatabase) {
    val clientRepo = ClientRepo(database.clientDao())
    val clientSessionRepo = ClientSessionRepo(database.clientSessionDao())
    val clientSettingRepo = ClientSettingRepo(database.clientSettingDao())
    val folderRepo = FolderRepo(database.folderDao())
    val playlistRepo = PlaylistRepo(database.playlistDao())
    val playlistTrackCrossRefRepo = PlaylistTrackCrossRefRepo(database.playlistTrackCrossRefDao())
    val trackRepo = TrackRepo(database.trackDao())
    val importSessionRepo = ImportSessionRepo(database.importSessionDao())
    val importSessionItemRepo = ImportSessionItemRepo(database.importSessionItemDao())
    val tagRepo = TagRepo(database.tagDao())
    val tagTrackCrossRefRepo = TagTrackCrossRefRepo(database.tagTrackCrossRefDao())
    val settingRepo = SettingRepo(database.settingDao())
    val spotifyAlbumRepo = SpotifyAlbumRepo(database.spotifyAlbumDao())
    val spotifyArtistRepo = SpotifyArtistRepo(database.spotifyArtistDao())
    val spotifyTrackRepo = SpotifyTrackRepo(database.spotifyTrackDao())
}