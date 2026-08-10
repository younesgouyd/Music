package dev.younesgouyd.apps.music.common.models.rpc

import dev.younesgouyd.apps.music.common.models.*
import kotlinx.serialization.Serializable

@Serializable
sealed class TrackRpc : Rpc() {
    @Serializable
    data class Get(
        val id: TrackId
    ) : TrackRpc()

    @Serializable
    data class Search(
        val nameQuery: String,
        val limit: Int,
        val offset: Offset.Id<TrackId>
    ) : TrackRpc()

    @Serializable
    data class SearchWithTags(
        val nameQuery: String,
        val tags: List<TagId>,
        val includeUntagged: Boolean,
        val limit: Int,
        val offset: Offset.Id<TrackId>
    ) : TrackRpc()

    @Serializable
    data class SearchFolder(
        val folderId: FolderId,
        val nameQuery: String
    ) : TrackRpc()

    @Serializable
    data class SearchFolderWithTags(
        val folderId: FolderId,
        val nameQuery: String,
        val tags: List<TagId>,
        val includeUntagged: Boolean
    ) : TrackRpc()

    @Serializable
    data class SearchArtistContributions(
        val id: SpotifyArtistId,
        val nameQuery: String,
        val limit: Int,
        val offset: Offset.Id<TrackId>
    ) : TrackRpc()

    @Serializable
    data class SearchPlaylist(
        val id: PlaylistId,
        val nameQuery: String
    ) : TrackRpc()

    @Serializable
    data class SearchWithTag(
        val nameQuery: String,
        val tag: TagId
    ) : TrackRpc()

    @Serializable
    data class GetFolderTracks(
        val id: FolderId
    ) : TrackRpc()

    @Serializable
    data class GetArtistTracks(
        val id: SpotifyArtistId
    ) : TrackRpc()

    @Serializable
    data class GetAlbumTracks(
        val id: SpotifyAlbumId
    ) : TrackRpc()

    @Serializable
    data class GetPlaylistTracks(
        val id: PlaylistId
    ) : TrackRpc()

    @Serializable
    data class GetId(
        val spotifyId: String
    ) : TrackRpc()

    @Serializable
    data class GetImportSessionTrack(
        val id: ImportSessionItemId
    ) : TrackRpc()

    @Serializable
    data class Add(
        val importSessionItemId: ImportSessionItemId,
        val spotifyTrackId: SpotifyTrackId?,
        val folderId: FolderId
    ) : TrackRpc()

    @Serializable
    data class UpdateFolderId(
        val id: TrackId,
        val folderId: FolderId
    ) : TrackRpc()
}