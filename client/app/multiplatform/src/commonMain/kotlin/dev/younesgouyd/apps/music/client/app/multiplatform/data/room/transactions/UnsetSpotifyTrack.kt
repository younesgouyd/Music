package dev.younesgouyd.apps.music.client.app.multiplatform.data.room.transactions

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import dev.younesgouyd.apps.music.client.app.multiplatform.data.*
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.SpotifyAlbum
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.SpotifyArtist
import io.github.oshai.kotlinlogging.KotlinLogging

@Dao
abstract class UnsetSpotifyTrack {
    private val logger = KotlinLogging.logger {}

    @Transaction
    open suspend fun execute(
        trackId: TrackId,
        spotifyTrackId: SpotifyTrackId,
        spotifyAlbumId: SpotifyAlbumId
    ): Set<MediaFileId> {
        logger.info { "--> ::execute" }
            // if (spotifyTrackId is the only one in spotifyAlbumId that is set to a Track) {
            //    delete spotifyAlbumId: {
            //        - delete SpotifyTracks: these will be deleted automatically
            //        - delete SpotifyArtists that will become not linked to any SpotifyAlbum or SpotifyTrack (see query)
            //        - delete all MediaFiles that are related to spotifyAlbumId and the artists mentioned above.
            //    }
            // }
        val removeAlbum = isTheOnlyOneSetToATrackInAlbum(trackId, spotifyAlbumId)
        updateSpotifyTrackId(null, System.currentTimeMillis(), trackId)
        if (removeAlbum) {
            logger.info { "::execute | removing album: $spotifyAlbumId" }
            val albumToDelete = getAlbum(spotifyAlbumId)
            val artistsToDelete = getArtistsToDelete(spotifyAlbumId)
            val mediaFilesToDelete: Set<MediaFileId> = buildSet {
                albumToDelete.largeImgId?.let { add(it) }
                albumToDelete.mediumImgId?.let { add(it) }
                albumToDelete.smallImgId?.let { add(it) }
                for (artist in artistsToDelete) {
                    artist.largeImgId?.let { add(it) }
                    artist.mediumImgId?.let { add(it) }
                    artist.smallImgId?.let { add(it) }
                }
            }
            deleteAlbum(spotifyAlbumId)
            deleteArtists(artistsToDelete.map { it.id }.toSet())
            deleteMediaFiles(mediaFilesToDelete)
            logger.info { "::execute | mediaFilesToDelete.size: ${mediaFilesToDelete.size}, mediaFilesToDelete: $mediaFilesToDelete" }
            return mediaFilesToDelete
        } else {
            return emptySet()
        }
    }

    @Query("select * from spotifyalbum where id = :id")
    protected abstract suspend fun getAlbum(id: SpotifyAlbumId): SpotifyAlbum

    @Query("""
        select sp.id as spotifyTrackId, t.id as trackId
        from spotifytrack sp
        left join track t on t.spotifyTrackId == sp.id
        where sp.spotifyAlbumId = :albumId
    """)
    protected abstract suspend fun getAlbumTracks(albumId: SpotifyAlbumId): List<SpotifyTrackIdWithTrackId>

    @Query("update track set spotifyTrackId = :spotifyTrackId, updateDatetime = :updateDatetime where id = :id")
    protected abstract suspend fun updateSpotifyTrackId(spotifyTrackId: SpotifyTrackId?, updateDatetime: Long, id: TrackId)

    @Query("delete from mediafile where id in (:ids)")
    protected abstract suspend fun deleteMediaFiles(ids: Set<MediaFileId>)

    @Query("delete from spotifyalbum where id = :id")
    protected abstract suspend fun deleteAlbum(id: SpotifyAlbumId)

    @Query("delete from spotifyartist where id in (:ids)")
    protected abstract suspend fun deleteArtists(ids: Set<SpotifyArtistId>)

    @Query("""
        -- SpotifyArtists that will become not linked to any SpotifyAlbum or SpotifyTrack
        select a.*
        from SpotifyArtist a
        where a.id not in ( -- not linked to any track other than tracks in albumToDelete
            select track_cr.spotifyArtistId
            from SpotifyArtistSpotifyTrackCrossRef track_cr
            where track_cr.spotifyTrackId not in (
                select spt.id from SpotifyTrack spt where spt.spotifyAlbumId = :albumToDelete
            )
        )
        and a.id not in ( -- not linked to any album other than albumToDelete
            select album_cr.spotifyArtistId
            from SpotifyArtistSpotifyAlbumCrossRef album_cr
            where album_cr.spotifyAlbumId != :albumToDelete
        )
    """)
    protected abstract suspend fun getArtistsToDelete(albumToDelete: SpotifyAlbumId): List<SpotifyArtist>

    private suspend fun isTheOnlyOneSetToATrackInAlbum(id: TrackId, albumId: SpotifyAlbumId): Boolean {
        val list = getAlbumTracks(albumId)
        val condition = list.count { it.trackId != null } == 1
        if (condition && list.count { it.trackId == id } != 1) { TODO() }
        return condition
    }

    protected data class SpotifyTrackIdWithTrackId(
        val spotifyTrackId: SpotifyTrackId,
        val trackId: TrackId?
    )
}