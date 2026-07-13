package dev.younesgouyd.apps.music.client.app.multiplatform.data.room.transactions

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import dev.younesgouyd.apps.music.client.app.multiplatform.data.*
import dev.younesgouyd.libs.music.client.spotifyapi.SpotifyApi
import dev.younesgouyd.libs.music.client.spotifyapi.models.Track
import dev.younesgouyd.libs.music.client.spotifyapi.models.common.ArtistId
import dev.younesgouyd.libs.music.client.spotifyapi.models.common.ImageObject
import dev.younesgouyd.libs.music.client.spotifyapi.models.common.SimplifiedArtistObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

@Dao
abstract class SetTrackMetadataFromSpotify {
    @Transaction
    open suspend fun addAlbumAndSet(
        trackId: TrackId,
        spotifyApiTrack: Track,
        spotifyApi: SpotifyApi,
        fileManager: FileManager
    ) {
        withContext(Dispatchers.IO) {
            val album = spotifyApi.getAlbum(spotifyApiTrack.album.id)
            val albumTracks = spotifyApi.getAlbumTracks(spotifyApiTrack.album.id)
            val (albumJson, albumObj) = album
            val spotifyArtistIds: Map<ArtistId, SpotifyArtistId?> = buildMap {
                for (artist in albumObj.artists) {
                    put(artist.id, getSpotifyArtistId(artist.id.value)?.let { SpotifyArtistId(it) })
                }
                for ((_, track) in albumTracks) {
                    for (artist in track.artists) {
                        put(artist.id, getSpotifyArtistId(artist.id.value)?.let { SpotifyArtistId(it) })
                    }
                }
            }
            val albumImgLarge: ByteArray? = albumObj.images.getImg(0)
            val albumImgMedium: ByteArray? = albumObj.images.getImg(1)
            val albumImgSmall: ByteArray? = albumObj.images.getImg(2)

            val insertedArtistIds = mutableMapOf<ArtistId, SpotifyArtistId>()
            spotifyApi.getArtists(
                artistIds = spotifyArtistIds.filter { it.value == null }.map { it.key }
            ).forEach { (artistJson, artistObj) ->
                val imageIds = ImageIds()
                val large: ByteArray? = artistObj.images.getImg(0)
                val medium: ByteArray? = artistObj.images.getImg(1)
                val small: ByteArray? = artistObj.images.getImg(2)
                if (large != null) {
                    imageIds.large = getNewMediaFileId()
                    fileManager.saveMediaFile(large, imageIds.large!!)
                }
                if (medium != null) {
                    imageIds.medium = getNewMediaFileId()
                    fileManager.saveMediaFile(medium, imageIds.medium!!)
                }
                if (small != null) {
                    imageIds.small = getNewMediaFileId()
                    fileManager.saveMediaFile(small, imageIds.small!!)
                }
                val id = SpotifyArtistId(
                    addSpotifyArtist(
                        spotifyId = artistObj.id.value,
                        name = artistObj.name,
                        smallImgId = imageIds.small,
                        mediumImgId = imageIds.medium,
                        largeImgId = imageIds.large,
                        apiResponse = artistJson,
                        creationDatetime = System.currentTimeMillis()
                    )
                )
                insertedArtistIds[artistObj.id] = id
            }

            val albumMediaFileIds = ImageIds()
            if (albumImgLarge != null) {
                albumMediaFileIds.large = getNewMediaFileId()
                fileManager.saveMediaFile(albumImgLarge, albumMediaFileIds.large!!)
            }
            if (albumImgMedium != null) {
                albumMediaFileIds.medium = getNewMediaFileId()
                fileManager.saveMediaFile(albumImgMedium, albumMediaFileIds.medium!!)
            }
            if (albumImgSmall != null) {
                albumMediaFileIds.small = getNewMediaFileId()
                fileManager.saveMediaFile(albumImgSmall, albumMediaFileIds.small!!)
            }
            val albumId = SpotifyAlbumId(
                addSpotifyAlbum(
                    spotifyId = albumObj.id.value,
                    name = albumObj.name,
                    albumType = albumObj.albumType,
                    releaseDate = albumObj.releaseDate,
                    releaseDatePrecision = albumObj.releaseDatePrecision,
                    smallImgId = albumMediaFileIds.small,
                    mediumImgId = albumMediaFileIds.medium,
                    largeImgId = albumMediaFileIds.large,
                    apiResponse = albumJson,
                    creationDatetime = System.currentTimeMillis()
                )
            )
            fun List<SimplifiedArtistObject>.toDbIds(): Set<SpotifyArtistId> {
                return buildSet {
                    for (artist in this@toDbIds) {
                        add(spotifyArtistIds[artist.id] ?: insertedArtistIds[artist.id] ?: TODO())
                    }
                }
            }
            albumObj.artists.toDbIds().forEach {
                addArtistAlbumCrossRef(it, albumId, System.currentTimeMillis())
            }
            var spotifyTrackIdToSet: SpotifyTrackId? = null
            for (track in albumTracks) {
                val (trackJson, trackObj) = track
                val spotifyTrackId =
                    SpotifyTrackId(
                        addSpotifyTrack(
                            spotifyId = trackObj.id.value,
                            name = trackObj.name,
                            spotifyAlbumId = albumId,
                            discNumber = trackObj.discNumber,
                            trackNumber = trackObj.trackNumber,
                            durationMs = trackObj.durationMs,
                            explicit = trackObj.explicit,
                            apiResponse = trackJson,
                            creationDatetime = System.currentTimeMillis()
                        )
                    )
                trackObj.artists.toDbIds().forEach {
                    addArtistTrackCrossRef(it, spotifyTrackId, System.currentTimeMillis())
                }
                if (trackObj.id == spotifyApiTrack.id) {
                    spotifyTrackIdToSet = spotifyTrackId
                }
            }
            if (spotifyTrackIdToSet == null) { TODO() }
            updateTrackSpotifyTrackId(spotifyTrackIdToSet, System.currentTimeMillis(), trackId)
        }
    }

    @Query("select id from spotifytrack where spotifyId = :spotifyId")
    abstract suspend fun getSpotifyTrackId(spotifyId: String): Long?

    @Query("select id from spotifyartist where spotifyId = :spotifyId")
    protected abstract suspend fun getSpotifyArtistId(spotifyId: String): Long?

    @Query("update track set spotifyTrackId = :spotifyTrackId, updateDatetime = :updateDatetime where id = :id")
    abstract suspend fun updateTrackSpotifyTrackId(spotifyTrackId: SpotifyTrackId, updateDatetime: Long, id: TrackId)

    @Query("""
        insert into SpotifyArtist (
            spotifyId, name, smallImgId, mediumImgId,
            largeImgId, apiResponse, creationDatetime
        ) values (
            :spotifyId, :name, :smallImgId, :mediumImgId,
            :largeImgId, :apiResponse, :creationDatetime
        )
    """)
    protected abstract suspend fun addSpotifyArtist(
        spotifyId: String,
        name: String?,
        smallImgId: MediaFileId?,
        mediumImgId: MediaFileId?,
        largeImgId: MediaFileId?,
        apiResponse: String,
        creationDatetime: Long
    ): Long


    @Query("""
        insert into SpotifyAlbum (
            spotifyId, name, albumType, releaseDate, releaseDatePrecision, smallImgId,
            mediumImgId, largeImgId, apiResponse, creationDatetime
        ) values (
            :spotifyId, :name, :albumType, :releaseDate, :releaseDatePrecision, :smallImgId,
            :mediumImgId, :largeImgId, :apiResponse, :creationDatetime
        )
    """)
    protected abstract suspend fun addSpotifyAlbum(
        spotifyId: String,
        name: String,
        albumType: String,
        releaseDate: String,
        releaseDatePrecision: String,
        smallImgId: MediaFileId?,
        mediumImgId: MediaFileId?,
        largeImgId: MediaFileId?,
        apiResponse: String,
        creationDatetime: Long
    ): Long

    @Query("""
        insert into SpotifyTrack (
            spotifyId, name, spotifyAlbumId, discNumber, trackNumber, durationMs,
            explicit, apiResponse, creationDatetime
        ) values (
            :spotifyId, :name, :spotifyAlbumId, :discNumber, :trackNumber, :durationMs, 
            :explicit, :apiResponse, :creationDatetime
        )
    """)
    protected abstract suspend fun addSpotifyTrack(
        spotifyId: String,
        name: String,
        spotifyAlbumId: SpotifyAlbumId,
        discNumber: Int?,
        trackNumber: Int?,
        durationMs: Int?,
        explicit: Boolean?,
        apiResponse: String,
        creationDatetime: Long
    ): Long

    @Query("""
        insert into SpotifyArtistSpotifyAlbumCrossRef (spotifyArtistId, spotifyAlbumId, creationDatetime)
        values (:spotifyArtistId, :spotifyAlbumId, :creationDatetime)
    """)
    protected abstract suspend fun addArtistAlbumCrossRef(
        spotifyArtistId: SpotifyArtistId,
        spotifyAlbumId: SpotifyAlbumId,
        creationDatetime: Long
    )

    @Query("""
        insert into SpotifyArtistSpotifyTrackCrossRef (spotifyArtistId, spotifyTrackId, creationDatetime)
        values (:spotifyArtistId, :spotifyTrackId, :creationDatetime)
    """)
    protected abstract suspend fun addArtistTrackCrossRef(
        spotifyArtistId: SpotifyArtistId,
        spotifyTrackId: SpotifyTrackId,
        creationDatetime: Long
    )

    @Query("insert into mediafile (creationDatetime) values (:creationDatetime)")
    protected abstract suspend fun addMediaFile(creationDatetime: Long): Long

    private suspend fun getNewMediaFileId(): MediaFileId {
        return MediaFileId(addMediaFile(System.currentTimeMillis()))
    }

    private data class ImageIds(
        var large: MediaFileId? = null,
        var medium: MediaFileId? = null,
        var small: MediaFileId? = null
    )

    private suspend fun List<ImageObject>?.getImg(index: Int): ByteArray? {
        return withContext(Dispatchers.IO) {
            this@getImg?.getOrNull(index)?.let {
                URI(it.url).toURL().readBytes()
            }
        }
    }
}
