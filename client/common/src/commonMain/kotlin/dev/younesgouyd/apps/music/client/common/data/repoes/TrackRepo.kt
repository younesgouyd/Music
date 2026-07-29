package dev.younesgouyd.apps.music.client.common.data.repoes

import dev.younesgouyd.apps.music.common.*
import io.ktor.client.*
import kotlinx.coroutines.flow.Flow

class TrackRepo(
    private val client: HttpClient
) {
    fun get(id: TrackId): Flow<TrackRelation?> {
        TODO()
//        return dao.get(id)
    }

    suspend fun search(
        nameQuery: String,
        limit: Int,
        offset: Offset.Id<TrackId>,
    ): List<TrackRelation> {
        TODO()
//        return dao.search(
//            nameQuery = nameQuery.toSearchQuery(),
//            limit = limit,
//            lastId = offset.value ?: TrackId(0)
//        )
    }

    suspend fun search(
        nameQuery: String,
        tags: List<TagId>,
        includeUntagged: Boolean,
        limit: Int,
        offset: Offset.Id<TrackId>,
    ): List<TrackRelation> {
        TODO()
//        return dao.search(
//            nameQuery = nameQuery.toSearchQuery(),
//            tags = tags,
//            includeUntagged = includeUntagged,
//            limit = limit,
//            lastId = offset.value ?: TrackId(0)
//        )
    }

    fun searchFolder(
        folderId: FolderId,
        nameQuery: String
    ): Flow<List<TrackRelation>> {
        TODO()
//        return dao.searchFolder(folderId, nameQuery.toSearchQuery())
    }

    fun searchFolder(
        folderId: FolderId,
        nameQuery: String,
        tags: List<TagId>,
        includeUntagged: Boolean
    ): Flow<List<TrackRelation>> {
        TODO()
//        return dao.searchFolder(folderId, nameQuery.toSearchQuery(), tags, includeUntagged)
    }

    suspend fun searchArtistContributions(
        id: SpotifyArtistId,
        nameQuery: String,
        limit: Int,
        offset: Offset.Id<TrackId>
    ): List<TrackRelation> {
        TODO()
//        return dao.searchArtistContributions(
//            id = id,
//            nameQuery = nameQuery.toSearchQuery(),
//            limit = limit,
//            lastId = offset.value ?: TrackId(
//                0
//            )
//        )
    }

    fun searchPlaylist(id: PlaylistId, nameQuery: String): Flow<List<PlaylistTrack>> {
        TODO()
//        return dao.searchPlaylist(id, nameQuery.toSearchQuery())
    }

    fun searchWithTag(nameQuery: String, tag: TagId): Flow<List<TrackRelation>> {
        TODO()
//        return dao.searchWithTag(nameQuery.toSearchQuery(), tag)
    }

    fun getFolderTracks(id: FolderId): Flow<List<Track>> {
        TODO()
//        return dao.getFolderTracks(id)
    }

    fun getArtistTracks(id: SpotifyArtistId): Flow<List<TrackRelation>> {
        TODO()
//        return dao.getArtistTracks(id)
    }

    fun getAlbumTracks(id: SpotifyAlbumId): Flow<List<TrackRelation>> {
        TODO()
//        return dao.getAlbumTracks(id)
    }

    fun getPlaylistTracks(id: PlaylistId): Flow<List<TrackRelation>> {
        TODO()
//        return dao.getPlaylistTracks(id)
    }

    suspend fun getId(spotifyId: String): TrackId? {
        TODO()
//        return dao.getId(spotifyId)?.let {
//            TrackId(
//                it
//            )
//        }
    }

    fun getImportSessionTrack(id: ImportSessionItemId): Flow<TrackRelation?> {
        TODO()
//        return dao.getImportSessionTrack(id)
    }

    suspend fun add(
        importSessionItemId: ImportSessionItemId,
        spotifyTrackId: SpotifyTrackId?,
        folderId: FolderId
    ): TrackId {
        TODO()
//        val currentTime = System.currentTimeMillis()
//        val id = dao.add(
//            importSessionItemId = importSessionItemId,
//            spotifyTrackId = spotifyTrackId,
//            folderId = folderId,
//            creationDatetime = currentTime,
//            updateDatetime = currentTime
//        )
//        return TrackId(id)
    }

    suspend fun updateFolderId(id: TrackId, folderId: FolderId) {
        TODO()
//        dao.updateFolderId(folderId, System.currentTimeMillis(), id)
    }
}