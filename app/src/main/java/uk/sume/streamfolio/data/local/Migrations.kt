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

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `articles` ADD COLUMN `groupId` TEXT DEFAULT NULL")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `bookmark_lists` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`isDefault` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL" +
                ")"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_bookmark_lists_name` ON `bookmark_lists` (`name`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `article_bookmark_lists` (" +
                "`articleLink` TEXT NOT NULL, " +
                "`listId` INTEGER NOT NULL, " +
                "PRIMARY KEY(`articleLink`, `listId`)" +
                ")"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_article_bookmark_lists_articleLink` ON `article_bookmark_lists` (`articleLink`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_article_bookmark_lists_listId` ON `article_bookmark_lists` (`listId`)")

        // Seed default "Saved" list (id = 1)
        db.execSQL("INSERT OR IGNORE INTO `bookmark_lists` (`id`, `name`, `isDefault`, `createdAt`) VALUES (1, 'Saved', 1, ${System.currentTimeMillis()})")

        // Migrate existing bookmarked articles to default "Saved" list
        db.execSQL("INSERT OR IGNORE INTO `article_bookmark_lists` (`articleLink`, `listId`) SELECT `link`, 1 FROM `articles` WHERE `isBookmarked` = 1")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
