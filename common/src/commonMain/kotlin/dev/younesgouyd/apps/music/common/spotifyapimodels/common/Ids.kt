package dev.younesgouyd.apps.music.common.spotifyapimodels.common

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class TrackId(val value: String) {
    override fun toString(): String {
        return value
    }
}

@Serializable
@JvmInline
value class ArtistId(val value: String) {
    override fun toString(): String {
        return value
    }
}

@Serializable
@JvmInline
value class AlbumId(val value: String) {
    override fun toString(): String {
        return value
    }
}
