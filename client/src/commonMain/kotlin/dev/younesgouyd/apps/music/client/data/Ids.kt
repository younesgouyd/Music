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
value class ArtistId(val value: Long) {
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
value class ArtistTrackCrossRefId(val value: Long) {
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
value class MediaFileArtistCrossRefId(val value: Long) {
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
value class MediaFilePlaylistCrossRefId(val value: Long) {
    override fun toString(): String {
        return value.toString()
    }
}

@Serializable
@JvmInline
value class MediaFileTrackCrossRefId(val value: Long) {
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