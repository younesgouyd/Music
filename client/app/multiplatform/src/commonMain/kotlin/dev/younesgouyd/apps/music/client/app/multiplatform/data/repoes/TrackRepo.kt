package dev.younesgouyd.apps.music.client.app.multiplatform.data.repoes

import dev.younesgouyd.apps.music.client.app.multiplatform.data.*
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.daos.TrackDao
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.PlaylistTrack
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.Track
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.TrackRelation
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.toSearchQuery
import dev.younesgouyd.apps.music.client.app.multiplatform.util.Offset
import kotlinx.coroutines.flow.Flow

class TrackRepo(
    private val dao: TrackDao
) {
    fun get(id: TrackId): Flow<TrackRelation?> {
        return dao.get(id)
    }

    suspend fun search(
        nameQuery: String,
        tags: List<TagId>,
        includeUntagged: Boolean,
        limit: Int,
        offset: Offset.Id<TrackId>,
    ): List<TrackRelation> {
        return dao.search(nameQuery.toSearchQuery(), tags, includeUntagged, limit, offset.value ?: TrackId(
            0
        )
        )
    }

    fun searchFolder(
        folderId: FolderId,
        nameQuery: String,
        tags: List<TagId>,
        includeUntagged: Boolean
    ): Flow<List<TrackRelation>> {
        return dao.searchFolder(folderId, nameQuery.toSearchQuery(), tags, includeUntagged)
    }

    suspend fun searchArtistContributions(
        id: SpotifyArtistId,
        nameQuery: String,
        limit: Int,
        offset: Offset.Id<TrackId>
    ): List<TrackRelation> {
        return dao.searchArtistContributions(
            id = id,
            nameQuery = nameQuery.toSearchQuery(),
            limit = limit,
            lastId = offset.value ?: TrackId(
                0
            )
        )
    }

    fun searchPlaylist(id: PlaylistId, nameQuery: String): Flow<List<PlaylistTrack>> {
        return dao.searchPlaylist(id, nameQuery.toSearchQuery())
    }

    fun searchWithTag(nameQuery: String, tag: TagId): Flow<List<TrackRelation>> {
        return dao.searchWithTag(nameQuery.toSearchQuery(), tag)
    }

    fun getFolderTracks(id: FolderId): Flow<List<Track>> {
        return dao.getFolderTracks(id)
    }

    fun getArtistTracks(id: SpotifyArtistId): Flow<List<TrackRelation>> {
        return dao.getArtistTracks(id)
    }

    fun getAlbumTracks(id: SpotifyAlbumId): Flow<List<TrackRelation>> {
        return dao.getAlbumTracks(id)
    }

    fun getPlaylistTracks(id: PlaylistId): Flow<List<TrackRelation>> {
        return dao.getPlaylistTracks(id)
    }

    suspend fun getId(spotifyId: String): TrackId? {
        return dao.getId(spotifyId)?.let {
            TrackId(
                it
            )
        }
    }

    fun getImportSessionTrack(id: ImportSessionItemId): Flow<TrackRelation?> {
        return dao.getImportSessionTrack(id)
    }

    suspend fun add(
        importSessionItemId: ImportSessionItemId,
        spotifyTrackId: SpotifyTrackId?,
        folderId: FolderId
    ): TrackId {
        val currentTime = System.currentTimeMillis()
        val id = dao.add(
            importSessionItemId = importSessionItemId,
            spotifyTrackId = spotifyTrackId,
            folderId = folderId,
            creationDatetime = currentTime,
            updateDatetime = currentTime
        )
        return TrackId(id)
    }

    suspend fun updateFolderId(id: TrackId, folderId: FolderId) {
        dao.updateFolderId(folderId, System.currentTimeMillis(), id)
    }
}