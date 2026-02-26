package dev.younesgouyd.apps.music.client.app.multiplatform.data.room

import androidx.room.*
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.daos.*
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.entities.*
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.transactions.ClearImportSessionItem
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.transactions.Import
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.transactions.SetTrackMetadataFromSpotify
import dev.younesgouyd.apps.music.client.app.multiplatform.data.room.transactions.UnsetSpotifyTrack

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

    abstract fun clearImportSessionItem(): ClearImportSessionItem
    abstract fun importTrx(): Import
    abstract fun setTrackMetadataFromSpotify(): SetTrackMetadataFromSpotify
    abstract fun unsetSpotifyTrack(): UnsetSpotifyTrack
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}