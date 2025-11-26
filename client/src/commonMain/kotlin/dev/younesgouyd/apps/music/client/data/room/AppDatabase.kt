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
        MediaFilePlaylistCrossRef::class
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

    // TODO: delete
//    @Dao
//    interface SpecialDao {
//        @Insert suspend fun insert1(artist: List<Artist>)
//        @Insert suspend fun insert2(artistTrackCrossRef: List<ArtistTrackCrossRef>)
//        @Insert suspend fun insert3(folder: List<Folder>)
//        @Insert suspend fun insert4(importSession: List<ImportSession>)
//        @Insert suspend fun insert5(importSessionItem: List<ImportSessionItem>)
//        @Insert suspend fun insert6(mediaFile: List<MediaFile>)
//        @Insert suspend fun insert7(playlist: List<Playlist>)
//        @Insert suspend fun insert8(playlistTrackCrossRef: List<PlaylistTrackCrossRef>)
//        @Insert suspend fun insert9(setting: List<Setting>)
//        @Insert suspend fun insert10(track: List<Track>)
//        @Insert suspend fun insert11(mediaFileTrackCrossRef: List<MediaFileTrackCrossRef>)
//        @Insert suspend fun insert12(mediaFileImportSessionItemCrossRef: List<MediaFileImportSessionItemCrossRef>)
//        @Insert suspend fun insert13(mediaFileArtistCrossRef: List<MediaFileArtistCrossRef>)
//        @Insert suspend fun insert14(mediaFilePlaylistCrossRef: List<MediaFilePlaylistCrossRef>)
//        @Insert suspend fun insert15(mediaFileImportSessionCrossRef: List<MediaFileImportSessionCrossRef>)
//    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}