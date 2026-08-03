package dev.younesgouyd.apps.music.client.common.data

import dev.younesgouyd.apps.music.client.common.data.repoes.*

class RepoStore(
    backend: Backend,
    fileManager: FileManager
) {
    val folderRepo = FolderRepo(backend)
    val importSessionItemRepo = ImportSessionItemRepo(backend)
    val importSessionRepo = ImportSessionRepo(backend)
    val inspectionRepo = InspectionRepo(backend)
    val mediaFileRepo = MediaFileRepo(backend, fileManager)
    val playlistRepo = PlaylistRepo(backend)
    val playlistTrackCrossRefRepo = PlaylistTrackCrossRefRepo(backend)
    val settingRepo = SettingRepo(backend)
    val spotifyAlbumRepo = SpotifyAlbumRepo(backend)
    val spotifyArtistRepo = SpotifyArtistRepo(backend)
    val spotifyAuthRepo = SpotifyAuthRepo(backend)
    val spotifySearchRepo = SpotifySearchRepo(backend)
    val spotifyTrackRepo = SpotifyTrackRepo(backend)
    val tagRepo = TagRepo(backend)
    val tagTrackCrossRefRepo = TagTrackCrossRefRepo(backend)
    val trackRepo = TrackRepo(backend)
}
