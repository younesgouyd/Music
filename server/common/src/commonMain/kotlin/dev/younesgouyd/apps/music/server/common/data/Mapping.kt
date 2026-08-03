package dev.younesgouyd.apps.music.server.common.data

import dev.younesgouyd.apps.music.server.common.data.room.entities.*

fun Folder.toModel() = dev.younesgouyd.apps.music.common.models.Folder(
    id = id,
    name = name,
    parentFolderId = parentFolderId,
    creationDatetime = creationDatetime,
    updateDatetime = updateDatetime
)

fun ImportSession.toModel() = dev.younesgouyd.apps.music.common.models.ImportSession(
    id = id,
    uri = uri,
    inspection = inspection,
    destinationFolderId = destinationFolderId,
    imgId = imgId,
    creationDatetime = creationDatetime
)

fun ImportSessionItem.toModel() = dev.younesgouyd.apps.music.common.models.ImportSessionItem(
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

fun MediaFile.toModel() = dev.younesgouyd.apps.music.common.models.MediaFile(
    id = id,
    fileName = fileName,
    creationDatetime = creationDatetime
)

fun Playlist.toModel() = dev.younesgouyd.apps.music.common.models.Playlist(
    id = id,
    name = name,
    folderId = folderId,
    creationDatetime = creationDatetime,
    updateDatetime = updateDatetime
)

fun PlaylistTrack.toModel() = dev.younesgouyd.apps.music.common.models.PlaylistTrack(
    track = track.toModel(),
    playlistTrackCrossRefId = playlistTrackCrossRefId,
    originalImport = originalImport.toModel(),
    spotifyTrack = spotifyTrack?.toModel(),
    playlistCrossRef = playlistCrossRef?.toModel()
)

fun PlaylistTrackCrossRef.toModel() = dev.younesgouyd.apps.music.common.models.PlaylistTrackCrossRef(
    id = id,
    playlistId = playlistId,
    trackId = trackId,
    position = position,
    creationDatetime = creationDatetime,
    updateDatetime = updateDatetime
)

fun Setting.toModel() = dev.younesgouyd.apps.music.common.models.Setting(
    id = id,
    name = name,
    value = value,
    creationDatetime = creationDatetime,
    updateDatetime = updateDatetime
)

fun SpotifyAlbum.toModel() = dev.younesgouyd.apps.music.common.models.SpotifyAlbum(
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

fun SpotifyArtist.toModel() = dev.younesgouyd.apps.music.common.models.SpotifyArtist(
    id = id,
    spotifyId = spotifyId,
    name = name,
    smallImgId = smallImgId,
    mediumImgId = mediumImgId,
    largeImgId = largeImgId,
    apiResponse = apiResponse,
    creationDatetime = creationDatetime
)

fun SpotifyTrack.toModel() = dev.younesgouyd.apps.music.common.models.SpotifyTrack(
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

fun SpotifyTrackRelation.toModel() = dev.younesgouyd.apps.music.common.models.SpotifyTrackRelation(
    spotifyTrack = spotifyTrack.toModel(),
    track = track?.toModel()
)

fun Tag.toModel() = dev.younesgouyd.apps.music.common.models.Tag(
    id = id,
    name = name,
    creationDatetime = creationDatetime
)

fun Track.toModel() = dev.younesgouyd.apps.music.common.models.Track(
    id = id,
    importSessionItemId = importSessionItemId,
    spotifyTrackId = spotifyTrackId,
    folderId = folderId,
    creationDatetime = creationDatetime,
    updateDatetime = updateDatetime
)

fun TrackRelation.toModel() = dev.younesgouyd.apps.music.common.models.TrackRelation(
    track = track.toModel(),
    originalImport = originalImport.toModel(),
    spotifyTrack = spotifyTrack?.toModel()
)