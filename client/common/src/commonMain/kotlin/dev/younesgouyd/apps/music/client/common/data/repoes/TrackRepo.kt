package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.rpc.TrackRpc
import kotlinx.coroutines.flow.Flow

class TrackRepo(
    private val backend: Backend
) {
    fun get(id: TrackId): Flow<TrackRelation?> {
        return backend.stream(TrackRpc.Get(id))
    }

    suspend fun search(
        nameQuery: String,
        limit: Int,
        offset: Offset.Id<TrackId>
    ): List<TrackRelation> {
        return backend.callForResult(
            TrackRpc.Search(
                nameQuery = nameQuery,
                limit = limit,
                offset = offset
            )
        )
    }

    suspend fun searchWithTags(
        nameQuery: String,
        tags: List<TagId>,
        includeUntagged: Boolean,
        limit: Int,
        offset: Offset.Id<TrackId>
    ): List<TrackRelation> {
        return backend.callForResult(
            TrackRpc.SearchWithTags(
                nameQuery = nameQuery,
                tags = tags,
                includeUntagged = includeUntagged,
                limit = limit,
                offset = offset
            )
        )
    }

    fun searchFolder(
        folderId: FolderId,
        nameQuery: String
    ): Flow<List<TrackRelation>> {
        return backend.stream(
            TrackRpc.SearchFolder(
                folderId = folderId,
                nameQuery = nameQuery
            )
        )
    }

    fun searchFolderWithTags(
        folderId: FolderId,
        nameQuery: String,
        tags: List<TagId>,
        includeUntagged: Boolean
    ): Flow<List<TrackRelation>> {
        return backend.stream(
            TrackRpc.SearchFolderWithTags(
                folderId = folderId,
                nameQuery = nameQuery,
                tags = tags,
                includeUntagged = includeUntagged
            )
        )
    }

    suspend fun searchArtistContributions(
        id: SpotifyArtistId,
        nameQuery: String,
        limit: Int,
        offset: Offset.Id<TrackId>
    ): List<TrackRelation> {
        return backend.callForResult(
            TrackRpc.SearchArtistContributions(
                id = id,
                nameQuery = nameQuery,
                limit = limit,
                offset = offset
            )
        )
    }

    fun searchPlaylist(id: PlaylistId, nameQuery: String): Flow<List<PlaylistTrack>> {
        return backend.stream(
            TrackRpc.SearchPlaylist(
                id = id,
                nameQuery = nameQuery
            )
        )
    }

    fun searchWithTag(nameQuery: String, tag: TagId): Flow<List<TrackRelation>> {
        return backend.stream(
            TrackRpc.SearchWithTag(
                nameQuery = nameQuery,
                tag = tag
            )
        )
    }

    fun getFolderTracks(id: FolderId): Flow<List<Track>> {
        return backend.stream(TrackRpc.GetFolderTracks(id))
    }

    fun getArtistTracks(id: SpotifyArtistId): Flow<List<TrackRelation>> {
        return backend.stream(TrackRpc.GetArtistTracks(id))
    }

    fun getAlbumTracks(id: SpotifyAlbumId): Flow<List<TrackRelation>> {
        return backend.stream(TrackRpc.GetAlbumTracks(id))
    }

    fun getPlaylistTracks(id: PlaylistId): Flow<List<TrackRelation>> {
        return backend.stream(TrackRpc.GetPlaylistTracks(id))
    }

    suspend fun getId(spotifyId: String): TrackId? {
        return backend.callForResult(
            TrackRpc.GetId(spotifyId)
        )
    }

    fun getImportSessionTrack(id: ImportSessionItemId): Flow<TrackRelation?> {
        return backend.stream(TrackRpc.GetImportSessionTrack(id))
    }

    suspend fun add(
        importSessionItemId: ImportSessionItemId,
        spotifyTrackId: SpotifyTrackId?,
        folderId: FolderId
    ): TrackId {
        return backend.callForResult(
            TrackRpc.Add(
                importSessionItemId = importSessionItemId,
                spotifyTrackId = spotifyTrackId,
                folderId = folderId
            )
        )
    }

    suspend fun updateFolderId(id: TrackId, folderId: FolderId) {
        backend.call(
            TrackRpc.UpdateFolderId(
                id = id,
                folderId = folderId
            )
        )
    }
}