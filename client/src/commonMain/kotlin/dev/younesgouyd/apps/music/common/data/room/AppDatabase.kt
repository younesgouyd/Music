package dev.younesgouyd.apps.music.common.data.room

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import dev.younesgouyd.apps.music.common.data.room.entities.*

@Database(
    entities = [
        Album::class,
        Artist::class,
        ArtistTrackCrossRef::class,
        Folder::class,
        ImportSession::class,
        ImportSessionItem::class,
        MediaFile::class,
        Playlist::class,
        PlaylistTrackCrossRef::class,
        Setting::class,
        Track::class
    ],
    version = 1
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun artistTrackCrossRefDao(): ArtistTrackCrossRefDao
    abstract fun folderDao(): FolderDao
    abstract fun importSessionDao(): ImportSessionDao
    abstract fun mediaFileDao(): MediaFileDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistTrackCrossRefDao(): PlaylistTrackCrossRefDao
    abstract fun settingDao(): SettingDao
    abstract fun trackDao(): TrackDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}