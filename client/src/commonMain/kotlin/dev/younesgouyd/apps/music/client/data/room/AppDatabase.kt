package dev.younesgouyd.apps.music.client.data.room

import androidx.room.*
import dev.younesgouyd.apps.music.client.data.room.entities.*

@Database(
    entities = [
        Artist::class,
        ArtistTrackCrossRef::class,
        Folder::class,
        ImportSession::class,
        ImportSessionItem::class,
        MediaFile::class,
        Playlist::class,
        PlaylistTrackCrossRef::class,
        Setting::class,
        Track::class,
        MediaFileTrackCrossRef::class,
        MediaFileImportSessionCrossRef::class,
        MediaFileImportSessionItemCrossRef::class,
        MediaFileArtistCrossRef::class,
        MediaFilePlaylistCrossRef::class,
        Tag::class,
        TagTrackCrossRef::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun artistDao(): ArtistDao
    abstract fun artistTrackCrossRefDao(): ArtistTrackCrossRefDao
    abstract fun folderDao(): FolderDao
    abstract fun importSessionDao(): ImportSessionDao
    abstract fun importSessionItemDao(): ImportSessionItemDao
    abstract fun importSessionWithItemsDao(): ImportSessionWithItemsDao
    abstract fun mediaFileDao(): MediaFileDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistTrackCrossRefDao(): PlaylistTrackCrossRefDao
    abstract fun settingDao(): SettingDao
    abstract fun trackDao(): TrackDao
    abstract fun mediaFileTrackCrossRefDao(): MediaFileTrackCrossRefDao
    abstract fun mediaFileImportSessionItemCrossRefDao(): MediaFileImportSessionItemCrossRefDao
    abstract fun mediaFileArtistCrossRefDao(): MediaFileArtistCrossRefDao
    abstract fun mediaFilePlaylistCrossRefDao(): MediaFilePlaylistCrossRefDao
    abstract fun mediaFileImportSessionCrossRefDao(): MediaFileImportSessionCrossRefDao
    abstract fun tagDao(): TagDao
    abstract fun tagTrackCrossRefDao(): TagTrackCrossRefDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}