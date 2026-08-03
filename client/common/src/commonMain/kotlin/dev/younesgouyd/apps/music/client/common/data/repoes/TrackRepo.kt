package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.client.common.data.Backend
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.rpc.TrackRpc
import io.ktor.client.call.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TrackRepo(
    private val backend: Backend
) {
    fun get(id: TrackId): Flow<TrackRelation?> {
        return flow {
            emit(
                backend.call(
                    TrackRpc.Get(id)
                ).body<TrackRelation?>()
            )
        }
    }

    suspend fun search(
        nameQuery: String,
        limit: Int,
        offset: Offset.Id<TrackId>
    ): List<TrackRelation> {
        return backend.call(
            TrackRpc.Search(
                nameQuery = nameQuery,
                limit = limit,
                offset = offset
            )
        ).body<List<TrackRelation>>()
    }

    suspend fun searchWithTags(
        nameQuery: String,
        tags: List<TagId>,
        includeUntagged: Boolean,
        limit: Int,
        offset: Offset.Id<TrackId>
    ): List<TrackRelation> {
        return backend.call(
            TrackRpc.SearchWithTags(
                nameQuery = nameQuery,
                tags = tags,
                includeUntagged = includeUntagged,
                limit = limit,
                offset = offset
            )
        ).body<List<TrackRelation>>()
    }

    fun searchFolder(
        folderId: FolderId,
        nameQuery: String
    ): Flow<List<TrackRelation>> {
        return flow {
            emit(
                backend.call(
                    TrackRpc.SearchFolder(
                        folderId = folderId,
                        nameQuery = nameQuery
                    )
                ).body<List<TrackRelation>>()
            )
        }
    }

    fun searchFolderWithTags(
        folderId: FolderId,
        nameQuery: String,
        tags: List<TagId>,
        includeUntagged: Boolean
    ): Flow<List<TrackRelation>> {
        return flow {
            emit(
                backend.call(
                    TrackRpc.SearchFolderWithTags(
                        folderId = folderId,
                        nameQuery = nameQuery,
                        tags = tags,
                        includeUntagged = includeUntagged
                    )
                ).body<List<TrackRelation>>()
            )
        }
    }

    suspend fun searchArtistContributions(
        id: SpotifyArtistId,
        nameQuery: String,
        limit: Int,
        offset: Offset.Id<TrackId>
    ): List<TrackRelation> {
        return backend.call(
            TrackRpc.SearchArtistContributions(
                id = id,
                nameQuery = nameQuery,
                limit = limit,
                offset = offset
            )
        ).body<List<TrackRelation>>()
    }

    fun searchPlaylist(id: PlaylistId, nameQuery: String): Flow<List<PlaylistTrack>> {
        return flow {
            emit(
                backend.call(
                    TrackRpc.SearchPlaylist(
                        id = id,
                        nameQuery = nameQuery
                    )
                ).body<List<PlaylistTrack>>()
            )
        }
    }

    fun searchWithTag(nameQuery: String, tag: TagId): Flow<List<TrackRelation>> {
        return flow {
            emit(
                backend.call(
                    TrackRpc.SearchWithTag(
                        nameQuery = nameQuery,
                        tag = tag
                    )
                ).body<List<TrackRelation>>()
            )
        }
    }

    fun getFolderTracks(id: FolderId): Flow<List<Track>> {
        return flow {
            emit(
                backend.call(
                    TrackRpc.GetFolderTracks(id)
                ).body<List<Track>>()
            )
        }
    }

    fun getArtistTracks(id: SpotifyArtistId): Flow<List<TrackRelation>> {
        return flow {
            emit(
                backend.call(
                    TrackRpc.GetArtistTracks(id)
                ).body<List<TrackRelation>>()
            )
        }
    }

    fun getAlbumTracks(id: SpotifyAlbumId): Flow<List<TrackRelation>> {
        return flow {
            emit(
                backend.call(
                    TrackRpc.GetAlbumTracks(id)
                ).body<List<TrackRelation>>()
            )
        }
    }

    fun getPlaylistTracks(id: PlaylistId): Flow<List<TrackRelation>> {
        return flow {
            emit(
                backend.call(
                    TrackRpc.GetPlaylistTracks(id)
                ).body<List<TrackRelation>>()
            )
        }
    }

    suspend fun getId(spotifyId: String): TrackId? {
        return backend.call(
            TrackRpc.GetId(spotifyId)
        ).body<TrackId?>()
    }

    fun getImportSessionTrack(id: ImportSessionItemId): Flow<TrackRelation?> {
        return flow {
            emit(
                backend.call(
                    TrackRpc.GetImportSessionTrack(id)
                ).body<TrackRelation?>()
            )
        }
    }

    suspend fun add(
        importSessionItemId: ImportSessionItemId,
        spotifyTrackId: SpotifyTrackId?,
        folderId: FolderId
    ): TrackId {
        return backend.call(
            TrackRpc.Add(
                importSessionItemId = importSessionItemId,
                spotifyTrackId = spotifyTrackId,
                folderId = folderId
            )
        ).body<TrackId>()
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