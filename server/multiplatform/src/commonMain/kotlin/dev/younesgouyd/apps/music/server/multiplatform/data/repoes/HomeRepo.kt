package dev.younesgouyd.apps.music.server.multiplatform.data.repoes

import androidx.room3.withReadTransaction
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.common.models.datamodels.home.HomeItems
import dev.younesgouyd.apps.music.common.models.pagination.Offset
import dev.younesgouyd.apps.music.common.models.pagination.Page
import dev.younesgouyd.apps.music.server.multiplatform.data.room.AppDatabase
import dev.younesgouyd.apps.music.server.multiplatform.data.room.daos.*
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.Playlist
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.SpotifyAlbum
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.SpotifyArtist
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.Tag
import dev.younesgouyd.apps.music.server.multiplatform.data.room.models.TrackRelation
import dev.younesgouyd.apps.music.server.multiplatform.data.toModel

class HomeRepo(
    private val database: AppDatabase,
    private val homeDao: HomeDao,
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val albumDao: SpotifyAlbumDao,
    private val artistDao: SpotifyArtistDao,
    private val tagDao: TagDao
) {
    suspend fun getPage(
        query: String,
        selectedTypes: Set<HomeItemType>,
        selectedTags: Set<TagId>,
        offset: Offset.Home?
    ): Page<HomeItems, Offset.Home> {
        val pageSize = 50
        return database.withReadTransaction {
            val stubs = homeDao.getFeedPage(
                query = query.trim(),
                filterByTypes = selectedTypes.isNotEmpty(),
                allowedTypes = selectedTypes,
                tagCount = selectedTags.size,
                tagIds = selectedTags,
                cursorDatetime = offset?.creationDatetime,
                cursorType = offset?.itemType,
                cursorId = offset?.itemId,
                limit = pageSize
            )

            if (stubs.isEmpty()) {
                return@withReadTransaction Page(nextOffset = null, items = emptyList())
            }

            val idsByType = stubs.groupBy({ it.itemType }, { it.itemId })

            val tracks = idsByType[HomeItemType.TRACK]?.let { trackDao.getByIds(it.map(::TrackId).toSet()) }.orEmpty()
            val playlists = idsByType[HomeItemType.PLAYLIST]?.let { playlistDao.getByIds(it.map(::PlaylistId).toSet()) }.orEmpty()
            val albums = idsByType[HomeItemType.ALBUM]?.let { albumDao.getByIds(it.map(::SpotifyAlbumId).toSet()) }.orEmpty()
            val artists = idsByType[HomeItemType.ARTIST]?.let { artistDao.getByIds(it.map(::SpotifyArtistId).toSet()) }.orEmpty()
            val tags = idsByType[HomeItemType.TAG]?.let { tagDao.getByIds(it.map(::TagId).toSet()) }.orEmpty()

            val last = stubs.last()
            val nextOffset = if (stubs.size < pageSize) null else Offset.Home(
                creationDatetime = last.creationDatetime,
                itemType = last.itemType,
                itemId = last.itemId
            )

            Page(
                nextOffset = nextOffset,
                items = listOf(
                    HomeItems(
                        tracks = tracks.map(TrackRelation::toModel),
                        playlists = playlists.map(Playlist::toModel),
                        albums = albums.map(SpotifyAlbum::toModel),
                        artists = artists.map(SpotifyArtist::toModel),
                        tags = tags.map(Tag::toModel),
                        orderedFeed = stubs
                    )
                )
            )
        }
    }
}