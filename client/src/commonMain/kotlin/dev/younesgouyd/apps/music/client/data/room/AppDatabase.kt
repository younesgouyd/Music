package dev.younesgouyd.apps.music.client.data.room

import androidx.room.*

@Database(
    entities = [
        dev.younesgouyd.apps.music.client.data.room.entities.Artist::class,
        dev.younesgouyd.apps.music.client.data.room.entities.ArtistTrackCrossRef::class,
        dev.younesgouyd.apps.music.client.data.room.entities.Folder::class,
        dev.younesgouyd.apps.music.client.data.room.entities.ImportSession::class,
        dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItem::class,
        dev.younesgouyd.apps.music.client.data.room.entities.MediaFile::class,
        dev.younesgouyd.apps.music.client.data.room.entities.Playlist::class,
        dev.younesgouyd.apps.music.client.data.room.entities.PlaylistTrackCrossRef::class,
        dev.younesgouyd.apps.music.client.data.room.entities.Setting::class,
        dev.younesgouyd.apps.music.client.data.room.entities.Track::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun artistDao(): dev.younesgouyd.apps.music.client.data.room.entities.ArtistDao
    abstract fun artistTrackCrossRefDao(): dev.younesgouyd.apps.music.client.data.room.entities.ArtistTrackCrossRefDao
    abstract fun folderDao(): dev.younesgouyd.apps.music.client.data.room.entities.FolderDao
    abstract fun importSessionDao(): dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionDao
    abstract fun importSessionItemDao(): dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionItemDao
    abstract fun importSessionWithItemsDao(): dev.younesgouyd.apps.music.client.data.room.entities.ImportSessionWithItemsDao
    abstract fun mediaFileDao(): dev.younesgouyd.apps.music.client.data.room.entities.MediaFileDao
    abstract fun playlistDao(): dev.younesgouyd.apps.music.client.data.room.entities.PlaylistDao
    abstract fun playlistTrackCrossRefDao(): dev.younesgouyd.apps.music.client.data.room.entities.PlaylistTrackCrossRefDao
    abstract fun settingDao(): dev.younesgouyd.apps.music.client.data.room.entities.SettingDao
    abstract fun trackDao(): dev.younesgouyd.apps.music.client.data.room.entities.TrackDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}