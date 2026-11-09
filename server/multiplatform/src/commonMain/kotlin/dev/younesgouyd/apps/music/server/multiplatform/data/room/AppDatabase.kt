package dev.younesgouyd.apps.music.server.multiplatform.data.room

import androidx.room3.*
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import dev.younesgouyd.apps.music.server.multiplatform.data.room.daos.*
import dev.younesgouyd.apps.music.server.multiplatform.data.room.entities.*
import dev.younesgouyd.apps.music.server.multiplatform.data.room.transactions.*

@Database(
    entities = [
        Client::class,
        ClientSession::class,
        ClientSetting::class,
        Folder::class,
        ImportSession::class,
        ImportSessionItem::class,
        MediaFile::class,
        PlaybackInterval::class,
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
    version = 2
)
@ColumnTypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun clientSessionDao(): ClientSessionDao
    abstract fun clientSettingDao(): ClientSettingDao
    abstract fun folderDao(): FolderDao
    abstract fun importSessionDao(): ImportSessionDao
    abstract fun importSessionItemDao(): ImportSessionItemDao
    abstract fun playbackIntervalDao(): PlaybackIntervalDao
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
    abstract fun prepareImportFromInternet(): PrepareImport
    abstract fun setTrackMetadataFromSpotify(): SetTrackMetadataFromSpotify
    abstract fun unsetSpotifyTrack(): UnsetSpotifyTrack
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        // copied from generated jvm code
        connection.execSQL("DROP TABLE IF EXISTS `PlaybackInterval`")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `PlaybackInterval` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` BLOB NOT NULL, `audioFileId` INTEGER NOT NULL, `possibleDuplicateOf` INTEGER, `clientId` INTEGER NOT NULL, `importId` INTEGER NOT NULL, `trackId` INTEGER NOT NULL, `playlistId` INTEGER, `folderId` INTEGER, `albumId` INTEGER, `artistId` INTEGER, `from` INTEGER NOT NULL, `to` INTEGER, `estimatedTo` INTEGER, `websocketPingPeriodMs` INTEGER NOT NULL, `websocketPingTimeoutMs` INTEGER NOT NULL, `startDatetime` INTEGER NOT NULL, `endDatetime` INTEGER NOT NULL, `creationDatetime` INTEGER NOT NULL, `updateDatetime` INTEGER NOT NULL, FOREIGN KEY(`audioFileId`) REFERENCES `MediaFile`(`id`) ON UPDATE CASCADE ON DELETE NO ACTION , FOREIGN KEY(`possibleDuplicateOf`) REFERENCES `PlaybackInterval`(`id`) ON UPDATE CASCADE ON DELETE NO ACTION , FOREIGN KEY(`clientId`) REFERENCES `Client`(`id`) ON UPDATE CASCADE ON DELETE NO ACTION , FOREIGN KEY(`importId`) REFERENCES `ImportSessionItem`(`id`) ON UPDATE CASCADE ON DELETE NO ACTION , FOREIGN KEY(`trackId`) REFERENCES `Track`(`id`) ON UPDATE CASCADE ON DELETE NO ACTION , FOREIGN KEY(`playlistId`) REFERENCES `Playlist`(`id`) ON UPDATE CASCADE ON DELETE NO ACTION , FOREIGN KEY(`folderId`) REFERENCES `Folder`(`id`) ON UPDATE CASCADE ON DELETE NO ACTION , FOREIGN KEY(`albumId`) REFERENCES `SpotifyAlbum`(`id`) ON UPDATE CASCADE ON DELETE NO ACTION , FOREIGN KEY(`artistId`) REFERENCES `SpotifyArtist`(`id`) ON UPDATE CASCADE ON DELETE NO ACTION )")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_PlaybackInterval_uuid` ON `PlaybackInterval` (`uuid`)")
    }
}