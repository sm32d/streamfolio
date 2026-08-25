package uk.sume.streamfolio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import uk.sume.streamfolio.data.model.Article
import uk.sume.streamfolio.data.model.ArticleBookmarkListCrossRef
import uk.sume.streamfolio.data.model.BookmarkList
import uk.sume.streamfolio.data.model.CustomFeed
import uk.sume.streamfolio.data.model.TtsPlaylistState

@Database(
    entities = [Article::class, CustomFeed::class, TtsPlaylistState::class, BookmarkList::class, ArticleBookmarkListCrossRef::class],
    version = 9,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun customFeedDao(): CustomFeedDao
    abstract fun ttsPlaylistStateDao(): TtsPlaylistStateDao
    abstract fun bookmarkListDao(): BookmarkListDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "news_database"
                )
                .addMigrations(*ALL_MIGRATIONS)
                // Allow destructive migration only from very old schema versions (1-4).
                // Current version 5 and all future versions must use explicit migrations
                // to prevent accidental data loss (bookmarks, reading history, etc.).
                .fallbackToDestructiveMigrationFrom(1, 2, 3, 4)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // MIGRATION_7_8 only seeds this default list for installs that were
                        // upgraded through schema v7->v8. A fresh install starts directly at the
                        // current version and never runs that migration, so bookmark_lists ends
                        // up with zero rows — and SaveToListBottomSheet then has nothing to show,
                        // silently failing to bookmark anything. Idempotently seeding it here on
                        // every open (not just onCreate) also self-heals any existing install that
                        // is already stuck in that empty state.
                        db.execSQL(
                            "INSERT OR IGNORE INTO `bookmark_lists` (`id`, `name`, `isDefault`, `createdAt`) VALUES (1, 'Saved', 1, ${System.currentTimeMillis()})"
                        )
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
