package dev.younesgouyd.apps.music.client.multiplatform.usecases

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.*

class TagItemsUseCase(
    private val server: Server
) {
    suspend fun execute(
        tracks: Set<TrackId> = emptySet(),
        playlists: Set<PlaylistId> = emptySet(),
        folders: Set<FolderId> = emptySet(),
        artists: Set<SpotifyArtistId> = emptySet(),
        albums: Set<SpotifyAlbumId> = emptySet(),
        tags: Set<TagId>,
        unset: Boolean
    ) {
        require(
            tracks.isNotEmpty() || playlists.isNotEmpty() || folders.isNotEmpty()
            || artists.isNotEmpty() || albums.isNotEmpty()
        )
        server.call(
            Rpc.ToServer.UseCase.TagItems(
                tracks = tracks,
                playlists = playlists,
                folders = folders,
                artists = artists,
                albums = albums,
                tags = tags,
                unset = unset
            )
        )
    }
}