package dev.younesgouyd.apps.music.client.data

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class TrackId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class FolderId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class PlaylistId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class ImportSessionId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class ImportSessionItemId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class MediaFileId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class MediaFileImportSessionCrossRefId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class MediaFileImportSessionItemCrossRefId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class PlaylistTrackCrossRefId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class SettingId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class TagId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class TagTrackCrossRefId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class SpotifyTrackId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class SpotifyArtistId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class SpotifyAlbumId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class SpotifyArtistSpotifyTrackCrossRefId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class SpotifyArtistSpotifyAlbumCrossRefId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class MediaFileSpotifyAlbumCrossRefId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class MediaFileSpotifyArtistCrossRefId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}