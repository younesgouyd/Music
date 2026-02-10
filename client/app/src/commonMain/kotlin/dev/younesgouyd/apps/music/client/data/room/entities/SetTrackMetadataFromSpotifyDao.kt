package dev.younesgouyd.apps.music.client.data.room.entities

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import dev.younesgouyd.apps.music.client.data.*
import dev.younesgouyd.libs.music.spotifyapi.SpotifyApi
import dev.younesgouyd.libs.music.spotifyapi.models.Artist
import dev.younesgouyd.libs.music.spotifyapi.models.Track
import dev.younesgouyd.libs.music.spotifyapi.models.common.ArtistId
import dev.younesgouyd.libs.music.spotifyapi.models.common.SimplifiedArtistObject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.net.URI

@Dao
abstract class SetTrackMetadataFromSpotifyDao {
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
            val artists: List<Pair<String, Artist>> = spotifyApi.getArtists(
                artistIds = spotifyArtistIds.filter { it.value == null }.map { it.key }
            )
            val artistImages: Map<ArtistId, ArtistImages> = buildMap {
                for ((_, artist) in artists) {
                    val large: Deferred<ByteArray> = async { URI(artist.images!![0].url).toURL().readBytes() }
                    val medium: Deferred<ByteArray> = async { URI(artist.images!![1].url).toURL().readBytes() }
                    val small: Deferred<ByteArray> = async { URI(artist.images!![2].url).toURL().readBytes() }
                    put(artist.id, ArtistImages(large.await(), medium.await(), small.await()))
                }
            }
            val albumImgLarge: ByteArray = URI(albumObj.images[0].url).toURL().readBytes()
            val albumImgMedium: ByteArray = URI(albumObj.images[1].url).toURL().readBytes()
            val albumImgSmall: ByteArray = URI(albumObj.images[2].url).toURL().readBytes()

            val artistMediaFileIds = mutableMapOf<ArtistId, ImageIds>()
            val insertedArtistIds = mutableMapOf<ArtistId, SpotifyArtistId>()
            for ((artistJson, artistObj) in artists) {
                val images = addImages()
                val id = SpotifyArtistId(
                    addSpotifyArtist(
                        spotifyId = artistObj.id.value,
                        name = artistObj.name,
                        smallImgId = images.small,
                        mediumImgId = images.medium,
                        largeImgId = images.large,
                        apiResponse = artistJson,
                        creationDatetime = System.currentTimeMillis()
                    )
                )
                insertedArtistIds[artistObj.id] = id
                artistMediaFileIds[artistObj.id] = images
            }
            val albumMediaFileIds = addImages()
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
                val spotifyTrackId = SpotifyTrackId(
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

            fileManager.saveMediaFile(albumImgLarge, albumMediaFileIds.large)
            fileManager.saveMediaFile(albumImgMedium, albumMediaFileIds.medium)
            fileManager.saveMediaFile(albumImgSmall, albumMediaFileIds.small)
            for ((artistId, imageIds) in artistMediaFileIds) {
                val images = artistImages[artistId] ?: TODO()
                fileManager.saveMediaFile(images.large, imageIds.large)
                fileManager.saveMediaFile(images.small, imageIds.small)
                fileManager.saveMediaFile(images.medium, imageIds.medium)
            }
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
        smallImgId: MediaFileId,
        mediumImgId: MediaFileId,
        largeImgId: MediaFileId,
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

    private suspend fun addImages() = ImageIds(
        large = MediaFileId(addMediaFile(System.currentTimeMillis())),
        medium = MediaFileId(addMediaFile(System.currentTimeMillis())),
        small = MediaFileId(addMediaFile(System.currentTimeMillis()))
    )

    private data class ImageIds(
        val large: MediaFileId,
        val medium: MediaFileId,
        val small: MediaFileId
    )

    private class ArtistImages(
        val large: ByteArray,
        val medium: ByteArray,
        val small: ByteArray
    )
}