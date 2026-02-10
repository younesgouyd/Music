package dev.younesgouyd.apps.music.client.data.room

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.room.entities.*

@Database(
    entities = [
        Folder::class,
        ImportSession::class,
        ImportSessionItem::class,
        MediaFile::class,
        Playlist::class,
        PlaylistTrackCrossRef::class,
        Setting::class,
        SpotifyAlbum::class,
        SpotifyArtist::class,
        SpotifyArtistSpotifyAlbumCrossRef::class,
        SpotifyArtistSpotifyTrackCrossRef::class,
        SpotifyTrack::class,
        Tag::class,
        TagTrackCrossRef::class,
        Track::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun importSessionDao(): ImportSessionDao
    abstract fun importSessionItemDao(): ImportSessionItemDao
    abstract fun mediaFileDao(): MediaFileDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistTrackCrossRefDao(): PlaylistTrackCrossRefDao
    abstract fun settingDao(): SettingDao
    abstract fun spotifyAlbumDao(): SpotifyAlbumDao
    abstract fun spotifyArtistDao(): SpotifyArtistDao
    abstract fun spotifyTrackDao(): SpotifyTrackDao
    abstract fun tagDao(): TagDao
    abstract fun tagTrackCrossRefDao(): TagTrackCrossRefDao
    abstract fun trackDao(): TrackDao

    abstract fun clearImportSessionItemDao(): ClearImportSessionItemDao
    abstract fun setTrackMetadataFromSpotifyDao(): SetTrackMetadataFromSpotifyDao
    abstract fun unsetSpotifyTrackDao(): UnsetSpotifyTrackDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}