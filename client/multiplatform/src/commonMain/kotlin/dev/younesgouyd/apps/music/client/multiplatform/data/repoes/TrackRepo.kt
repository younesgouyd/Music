package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.datamodels.PlaylistTrack
import dev.younesgouyd.apps.music.common.models.datamodels.Track
import dev.younesgouyd.apps.music.common.models.datamodels.TrackRelation
import kotlinx.coroutines.flow.Flow

class TrackRepo(
    private val server: Server
) {
    fun get(id: TrackId): Flow<TrackRelation?> {
        return server.observe(Rpc.ToServer.Track.Get(id))
    }

    suspend fun search(
        nameQuery: String,
        limit: Int,
        offset: Offset.Id<TrackId>
    ): List<TrackRelation> {
        return server.get(
            Rpc.ToServer.Track.Search(
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
        return server.get(
            Rpc.ToServer.Track.SearchWithTags(
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
        return server.observe(
            Rpc.ToServer.Track.SearchFolder(
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
        return server.observe(
            Rpc.ToServer.Track.SearchFolderWithTags(
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
        return server.get(
            Rpc.ToServer.Track.SearchArtistContributions(
                id = id,
                nameQuery = nameQuery,
                limit = limit,
                offset = offset
            )
        )
    }

    fun searchPlaylist(id: PlaylistId, nameQuery: String): Flow<List<PlaylistTrack>> {
        return server.observe(
            Rpc.ToServer.Track.SearchPlaylist(
                id = id,
                nameQuery = nameQuery
            )
        )
    }

    fun searchWithTag(nameQuery: String, tag: TagId): Flow<List<TrackRelation>> {
        return server.observe(
            Rpc.ToServer.Track.SearchWithTag(
                nameQuery = nameQuery,
                tag = tag
            )
        )
    }

    fun getFolderTracks(id: FolderId): Flow<List<Track>> {
        return server.observe(Rpc.ToServer.Track.GetFolderTracks(id))
    }

    fun getArtistTracks(id: SpotifyArtistId): Flow<List<TrackRelation>> {
        return server.observe(Rpc.ToServer.Track.GetArtistTracks(id))
    }

    fun getAlbumTracks(id: SpotifyAlbumId): Flow<List<TrackRelation>> {
        return server.observe(Rpc.ToServer.Track.GetAlbumTracks(id))
    }

    fun getPlaylistTracks(id: PlaylistId): Flow<List<TrackRelation>> {
        return server.observe(Rpc.ToServer.Track.GetPlaylistTracks(id))
    }

    suspend fun getId(spotifyId: String): TrackId? {
        return server.get(
            Rpc.ToServer.Track.GetId(spotifyId)
        )
    }

    fun getImportSessionTrack(id: ImportSessionItemId): Flow<TrackRelation?> {
        return server.observe(Rpc.ToServer.Track.GetImportSessionTrack(id))
    }

    suspend fun add(
        importSessionItemId: ImportSessionItemId,
        spotifyTrackId: SpotifyTrackId?,
        folderId: FolderId
    ): TrackId {
        return server.get(
            Rpc.ToServer.Track.Add(
                importSessionItemId = importSessionItemId,
                spotifyTrackId = spotifyTrackId,
                folderId = folderId
            )
        )
    }

    suspend fun updateFolderId(id: TrackId, folderId: FolderId) {
        server.call(
            Rpc.ToServer.Track.UpdateFolderId(
                id = id,
                folderId = folderId
            )
        )
    }
}