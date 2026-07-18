package uk.sume.streamfolio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import uk.sume.streamfolio.data.model.Article
import uk.sume.streamfolio.data.model.CustomFeed
import uk.sume.streamfolio.data.model.TtsPlaylistState

@Database(
    entities = [Article::class, CustomFeed::class, TtsPlaylistState::class],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun customFeedDao(): CustomFeedDao
    abstract fun ttsPlaylistStateDao(): TtsPlaylistStateDao

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
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
