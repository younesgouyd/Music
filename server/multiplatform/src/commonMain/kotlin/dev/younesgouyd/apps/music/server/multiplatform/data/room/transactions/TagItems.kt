package dev.younesgouyd.apps.music.server.multiplatform.data.room.transactions

import androidx.room3.*
import dev.younesgouyd.apps.music.common.models.*
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.TagTrackCrossRef
import io.github.oshai.kotlinlogging.KotlinLogging

@Dao
abstract class TagItems {
    private val logger = KotlinLogging.logger {  }

    @Transaction
    open suspend fun execute(
        tracks: Set<TrackId>,
        playlists: Set<PlaylistId>,
        folders: Set<FolderId>,
        artists: Set<SpotifyArtistId>,
        albums: Set<SpotifyAlbumId>,
        tags: Set<TagId>,
        unset: Boolean
    ) {
        if (tags.isEmpty()) {
            logger.warn { "empty tag set. will do nothing" }
            return
        }
        val toBeTagged = mutableSetOf<Long>()
        toBeTagged.addAll(tracks.map { it.value })
        for (playlist in playlists) { toBeTagged.addAll(getPlaylistTracks(playlist)) }
        for (folder in folders) { toBeTagged.addAll(getFolderTracksRecursive(folder)) }
        for (artist in artists) { toBeTagged.addAll(getArtistTracks(artist)) }
        for (album in albums) { toBeTagged.addAll(getAlbumTracks(album)) }
        if (toBeTagged.isEmpty()) return
        val currentTime = System.currentTimeMillis()
        if (unset) {
            val crossRefs = tags.flatMap { tag ->
                toBeTagged.map { trackId ->
                    TagAndTrackIds(
                        tagId = tag,
                        trackId = TrackId(trackId)
                    )
                }
            }
            deleteAll(crossRefs)
        } else {
            val crossRefs = tags.flatMap { tag ->
                toBeTagged.map { trackId ->
                    TagTrackCrossRef(
                        tagId = tag,
                        trackId = TrackId(trackId),
                        creationDatetime = currentTime
                    )
                }
            }
            addAll(crossRefs)
        }
    }

    data class TagAndTrackIds(val tagId: TagId, val trackId: TrackId)

    @Delete(entity = TagTrackCrossRef::class)
    abstract suspend fun deleteAll(crossRefs: List<TagAndTrackIds>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addAll(crossRefs: List<TagTrackCrossRef>)

    @Query("""
        with recursive folders(id) as (
            select id from folder where id = :folderId
            union
            select f.id from folder f join folders fs on f.parentFolderId = fs.id
        )
        select id from track where folderId in (select id from folders)
        union
        select cr.trackId
        from playlist p
        join playlisttrackcrossref cr on cr.playlistId = p.id
        where p.folderId in (select id from folders)
    """)
    protected abstract suspend fun getFolderTracksRecursive(folderId: FolderId): List<Long>

    @Query("select trackId from playlisttrackcrossref where playlistId = :id")
    abstract suspend fun getPlaylistTracks(id: PlaylistId): List<Long>

    @Query("""
        select t.id
        from track t
        join spotifyartistspotifytrackcrossref cr on cr.spotifyTrackId = t.spotifyTrackId
        where cr.spotifyArtistId = :id
    """)
    abstract suspend fun getArtistTracks(id: SpotifyArtistId): List<Long>

    @Query("""
        select t.id
        from track t
        join spotifytrack sp on sp.id = t.spotifyTrackId
        where sp.spotifyAlbumId = :id
    """)
    abstract suspend fun getAlbumTracks(id: SpotifyAlbumId): List<Long>
}