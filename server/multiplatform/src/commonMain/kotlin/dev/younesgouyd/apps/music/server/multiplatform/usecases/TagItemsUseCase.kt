package dev.younesgouyd.apps.music.server.multiplatform.usecases

import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.server.multiplatform.data.room.transactions.TagItems

class TagItemsUseCase(
    private val transaction: TagItems
) {
    suspend fun execute(
        tracks: Set<TrackId>,
        playlists: Set<PlaylistId>,
        folders: Set<FolderId>,
        artists: Set<SpotifyArtistId>,
        albums: Set<SpotifyAlbumId>,
        tags: Set<TagId>,
        unset: Boolean
    ) {
        transaction.execute(
            tracks = tracks,
            playlists = playlists,
            folders = folders,
            artists = artists,
            albums = albums,
            tags = tags,
            unset = unset
        )
    }
}