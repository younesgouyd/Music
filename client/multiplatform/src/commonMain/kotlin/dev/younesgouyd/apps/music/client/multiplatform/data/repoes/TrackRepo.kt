package dev.younesgouyd.apps.music.client.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.multiplatform.data.Server
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.datamodels.PlaylistTrack
import dev.younesgouyd.apps.music.common.models.datamodels.TrackRelation
import dev.younesgouyd.apps.music.common.models.pagination.Offset
import kotlinx.coroutines.flow.Flow

class TrackRepo(
    private val server: Server
) {
    fun get(id: TrackId): Flow<TrackRelation?> {
        return server.observe(Rpc.ToServer.Track.Get(id))
    }

    fun searchFolder(
        folderId: FolderId?,
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
        folderId: FolderId?,
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
}