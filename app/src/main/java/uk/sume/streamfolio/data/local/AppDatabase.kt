package uk.sume.streamfolio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import uk.sume.streamfolio.data.model.Article
import uk.sume.streamfolio.data.model.CustomFeed
import uk.sume.streamfolio.data.model.PodcastSubscription
import uk.sume.streamfolio.data.model.PodcastEpisode

@Database(entities = [Article::class, CustomFeed::class, PodcastSubscription::class, PodcastEpisode::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun customFeedDao(): CustomFeedDao
    abstract fun podcastDao(): PodcastDao

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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
