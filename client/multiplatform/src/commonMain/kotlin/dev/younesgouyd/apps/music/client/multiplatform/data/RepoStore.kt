package dev.younesgouyd.apps.music.client.multiplatform.data

import dev.younesgouyd.apps.music.client.multiplatform.data.repoes.*

class RepoStore(server: Server) {
    val folderRepo = FolderRepo(server)
    val importSessionItemRepo = ImportSessionItemRepo(server)
    val importSessionRepo = ImportSessionRepo(server)
    val inspectionRepo = InspectionRepo(server)
    val playlistRepo = PlaylistRepo(server)
    val playlistTrackCrossRefRepo = PlaylistTrackCrossRefRepo(server)
    val settingRepo = SettingRepo(server)
    val spotifyAlbumRepo = SpotifyAlbumRepo(server)
    val spotifyArtistRepo = SpotifyArtistRepo(server)
    val spotifyAuthRepo = SpotifyAuthRepo(server)
    val spotifySearchRepo = SpotifySearchRepo(server)
    val spotifyTrackRepo = SpotifyTrackRepo(server)
    val tagRepo = TagRepo(server)
    val tagTrackCrossRefRepo = TagTrackCrossRefRepo(server)
    val trackRepo = TrackRepo(server)
}
