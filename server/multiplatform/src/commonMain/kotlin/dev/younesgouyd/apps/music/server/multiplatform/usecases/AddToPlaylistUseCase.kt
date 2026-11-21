package dev.younesgouyd.apps.music.server.multiplatform.usecases

import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.server.multiplatform.data.room.transactions.AddToPlaylist

class AddToPlaylistUseCase(
    private val transaction: AddToPlaylist
) {
    suspend fun execute(
        tracks: Set<TrackId>,
        playlists: Set<PlaylistId>,
        folders: Set<FolderId>,
        artists: Set<SpotifyArtistId>,
        albums: Set<SpotifyAlbumId>,
        playlist: PlaylistToAddTo
    ) {
        transaction.execute(
            tracks = tracks,
            playlists = playlists,
            folders = folders,
            artists = artists,
            albums = albums,
            playlist = playlist
        )
    }
}