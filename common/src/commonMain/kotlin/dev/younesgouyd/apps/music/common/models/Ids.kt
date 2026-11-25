package dev.younesgouyd.apps.music.common.models

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ClientId(val value: Long) {
    val key get() = "ClientId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class ClientSessionId(val value: Long) {
    val key get() = "ClientSessionId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class ClientSettingId(val value: Long) {
    val key get() = "ClientSettingId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class TrackId(val value: Long) {
    val key get() = "TrackId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class FolderId(val value: Long) {
    val key get() = "FolderId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class PlaylistId(val value: Long) {
    val key get() = "PlaylistId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class ImportSessionId(val value: Long) {
    val key get() = "ImportSessionId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class ImportSessionItemId(val value: Long) {
    val key get() = "ImportSessionItemId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class MediaFileId(val value: Long) {
    val key get() = "MediaFileId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class PlaybackIntervalId(val value: Long) {
    val key get() = "PlaybackIntervalId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class PlaylistTrackCrossRefId(val value: Long) {
    val key get() = "PlaylistTrackCrossRefId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class SettingId(val value: Long) {
    val key get() = "SettingId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class TagId(val value: Long) {
    val key get() = "TagId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class TagTrackCrossRefId(val value: Long) {
    val key get() = "TagTrackCrossRefId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class SpotifyTrackId(val value: Long) {
    val key get() = "SpotifyTrackId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class SpotifyArtistId(val value: Long) {
    val key get() = "SpotifyArtistId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class SpotifyAlbumId(val value: Long) {
    val key get() = "SpotifyAlbumId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class SpotifyArtistSpotifyTrackCrossRefId(val value: Long) {
    val key get() = "SpotifyArtistSpotifyTrackCrossRefId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class SpotifyArtistSpotifyAlbumCrossRefId(val value: Long) {
    val key get() = "SpotifyArtistSpotifyAlbumCrossRefId#$value"
    init { require(value > 0) }
    override fun toString(): String {
        return value.toString()
    }
}