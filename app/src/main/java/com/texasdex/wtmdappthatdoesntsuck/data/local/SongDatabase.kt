package com.texasdex.wtmdappthatdoesntsuck.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SongEntity::class],
    version = 6,
    exportSchema = true
)
abstract class SongDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE liked_songs ADD COLUMN localCoverArtPath TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS liked_songs_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "songId TEXT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "artist TEXT NOT NULL, " +
                        "timestamp TEXT NOT NULL, " +
                        "coverArt TEXT, " +
                        "likedAtYear INTEGER NOT NULL, " +
                        "localCoverArtPath TEXT)")
                
                database.execSQL("INSERT INTO liked_songs_new (songId, title, artist, timestamp, coverArt, likedAtYear, localCoverArtPath) " +
                        "SELECT songId, title, artist, timestamp, coverArt, likedAtYear, localCoverArtPath FROM liked_songs")
                
                database.execSQL("DROP TABLE liked_songs")
                database.execSQL("ALTER TABLE liked_songs_new RENAME TO liked_songs")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE liked_songs ADD COLUMN addedAt INTEGER NOT NULL DEFAULT 0")
                // For existing records, use the auto-increment ID as a proxy for time if addedAt is 0
                database.execSQL("UPDATE liked_songs SET addedAt = id WHERE addedAt = 0")
            }
        }
    }
}
