package dev.younesgouyd.apps.music.server.multiplatform.data

import dev.younesgouyd.apps.music.common.models.datamodels.Setting
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.*
import dev.younesgouyd.apps.music.server.multiplatform.data.room.models.PlaylistTrack
import dev.younesgouyd.apps.music.server.multiplatform.data.room.models.SpotifyTrackRelation
import dev.younesgouyd.apps.music.server.multiplatform.data.room.models.TrackRelation

fun Folder.toModel() = dev.younesgouyd.apps.music.common.models.datamodels.Folder(
    id = id,
    name = name,
    parentFolderId = parentFolderId,
    creationDatetime = creationDatetime,
    updateDatetime = updateDatetime
)

fun ImportSession.toModel() = dev.younesgouyd.apps.music.common.models.datamodels.ImportSession(
    id = id,
    uri = uri,
    inspection = inspection,
    destinationFolderId = destinationFolderId,
    imgId = imgId,
    creationDatetime = creationDatetime
)

fun ImportSessionItem.toModel() = dev.younesgouyd.apps.music.common.models.datamodels.ImportSessionItem(
    id = id,
    uri = uri,
    importSessionId = importSessionId,
    state = state,
    title = title,
    durationMilliseconds = durationMilliseconds,
    album = album,
    inspection = inspection,
    imgId = imgId,
    audioFileId = audioFileId,
    creationDatetime = creationDatetime,
    updateDatetime = updateDatetime
)

fun MediaFile.toModel() = dev.younesgouyd.apps.music.common.models.datamodels.MediaFile(
    id = id,
    fileName = fileName,
    creationDatetime = creationDatetime
)

fun Playlist.toModel() = dev.younesgouyd.apps.music.common.models.datamodels.Playlist(
    id = id,
    name = name,
    folderId = folderId,
    creationDatetime = creationDatetime,
    updateDatetime = updateDatetime
)

fun PlaylistTrack.toModel() = dev.younesgouyd.apps.music.common.models.datamodels.PlaylistTrack(
    trackRelation = trackRelation.toModel(),
    playlistTrackCrossRefId = playlistTrackCrossRefId
)

fun PlaylistTrackCrossRef.toModel() = dev.younesgouyd.apps.music.common.models.datamodels.PlaylistTrackCrossRef(
    id = id,
    playlistId = playlistId,
    trackId = trackId,
    position = position,
    creationDatetime = creationDatetime,
    updateDatetime = updateDatetime
)

fun ClientSetting.toModel() = Setting(
    id = id,
    name = name,
    value = value,
    creationDatetime = creationDatetime,
    updateDatetime = updateDatetime
)

fun SpotifyAlbum.toModel() = dev.younesgouyd.apps.music.common.models.datamodels.SpotifyAlbum(
    id = id,
    spotifyId = spotifyId,
    name = name,
    albumType = albumType,
    releaseDate = releaseDate,
    releaseDatePrecision = releaseDatePrecision,
    smallImgId = smallImgId,
    mediumImgId = mediumImgId,
    largeImgId = largeImgId,
    apiResponse = apiResponse,
    creationDatetime = creationDatetime
)

fun SpotifyArtist.toModel() = dev.younesgouyd.apps.music.common.models.datamodels.SpotifyArtist(
    id = id,
    spotifyId = spotifyId,
    name = name,
    smallImgId = smallImgId,
    mediumImgId = mediumImgId,
    largeImgId = largeImgId,
    apiResponse = apiResponse,
    creationDatetime = creationDatetime
)

fun SpotifyTrack.toModel() = dev.younesgouyd.apps.music.common.models.datamodels.SpotifyTrack(
    id = id,
    spotifyId = spotifyId,
    name = name,
    spotifyAlbumId = spotifyAlbumId,
    discNumber = discNumber,
    trackNumber = trackNumber,
    durationMs = durationMs,
    explicit = explicit,
    apiResponse = apiResponse,
    creationDatetime = creationDatetime
)

fun SpotifyTrackRelation.toModel() = dev.younesgouyd.apps.music.common.models.datamodels.SpotifyTrackRelation(
    spotifyTrack = spotifyTrack.toModel(),
    spotifyAlbum = spotifyAlbum.toModel(),
    spotifyArtists = spotifyArtists.map { it.toModel() },
    track = track?.toModel()
)

fun Tag.toModel() = dev.younesgouyd.apps.music.common.models.datamodels.Tag(
    id = id,
    name = name,
    creationDatetime = creationDatetime
)

fun Track.toModel() = dev.younesgouyd.apps.music.common.models.datamodels.Track(
    id = id,
    importSessionItemId = importSessionItemId,
    spotifyTrackId = spotifyTrackId,
    folderId = folderId,
    creationDatetime = creationDatetime,
    updateDatetime = updateDatetime
)

fun TrackRelation.toModel() = dev.younesgouyd.apps.music.common.models.datamodels.TrackRelation(
    track = track.toModel(),
    originalImport = originalImport.toModel(),
    spotifyTrackRelation = spotifyTrackRelation?.toModel()
)