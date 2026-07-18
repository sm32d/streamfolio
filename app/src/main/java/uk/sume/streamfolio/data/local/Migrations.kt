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

val ALL_MIGRATIONS = arrayOf<Migration>()

// Template for the next migration:
//
// val MIGRATION_5_6 = object : Migration(5, 6) {
//     override fun migrate(db: SupportSQLiteDatabase) {
//         db.execSQL("ALTER TABLE articles ADD COLUMN some_new_field TEXT")
//     }
// }
//
// Then update: val ALL_MIGRATIONS = arrayOf(MIGRATION_5_6)
