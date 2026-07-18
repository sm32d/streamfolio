package uk.sume.streamfolio.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migrations for the StreamFolio database.
 *
 * IMPORTANT: When bumping @Database version, add a corresponding Migration(N, N+1)
 * here and include it in ALL_MIGRATIONS. Never use fallbackToDestructiveMigration()
 * for current or future versions — user data (bookmarks, reading history, AI
 * summaries, custom feeds) must be preserved across upgrades.
 *
 * Historical note: versions 1-4 pre-date this migration infrastructure and may
 * still be encountered on very old installs. Those versions are allowed to be
 * destructively migrated because their schemas are not available.
 */

/**
 * Adds the tts_playlist_state table used to persist the audio queue across
 * process death.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tts_playlist_state` (" +
                "`id` INTEGER NOT NULL, " +
                "`currentIndex` INTEGER NOT NULL, " +
                "`linksJson` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`)" +
                ")"
        )
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_5_6)
