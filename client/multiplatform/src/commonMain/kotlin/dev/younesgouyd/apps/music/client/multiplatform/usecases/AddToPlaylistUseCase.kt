package dev.younesgouyd.apps.music.client.multiplatform.usecases

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.*

class AddToPlaylistUseCase(
    private val server: Server
) {
    suspend fun execute(
        tracks: Set<TrackId>,
        playlists: Set<PlaylistId>,
        folders: Set<FolderId>,
        artists: Set<SpotifyArtistId>,
        albums: Set<SpotifyAlbumId>,
        playlist: PlaylistToAddTo
    ) {
        server.call(
            Rpc.ToServer.UseCase.AddToPlaylist(
                tracks = tracks,
                playlists = playlists,
                folders = folders,
                artists = artists,
                albums = albums,
                playlist = playlist
            )
        )
    }
}