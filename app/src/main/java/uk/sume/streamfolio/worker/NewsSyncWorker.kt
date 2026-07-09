package uk.sume.streamfolio.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import uk.sume.streamfolio.data.local.AppDatabase
import uk.sume.streamfolio.data.local.PreferencesHelper
import uk.sume.streamfolio.data.network.NewsRepository

class NewsSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("NewsSyncWorker", "Background sync worker started execution")
        val repository = NewsRepository(applicationContext)
        val prefs = PreferencesHelper(applicationContext)
        val db = AppDatabase.getDatabase(applicationContext)
        
        try {
            val categoriesToSync = prefs.selectedCategories.toList()
            val defaultCategories = setOf("Top Stories", "World", "Business", "Technology", "Science", "Sports", "Health", "Entertainment")
            val customFeeds = db.customFeedDao().getAllFeedsSync()

            for (cat in categoriesToSync) {
                try {
                    val matchingFeeds = customFeeds.filter { it.category == cat }
                    if (matchingFeeds.isNotEmpty()) {
                        repository.fetchCustomFeeds(matchingFeeds, cat)
                    }
                    if (defaultCategories.contains(cat) && prefs.isDefaultFeedsEnabled) {
                        repository.fetchDefaultFeeds(
                            category = cat,
                            language = prefs.language,
                            region = prefs.region,
                            disabledFeedUrls = prefs.disabledFeedUrls,
                            enabledCrossRegionFeeds = prefs.enabledCrossRegionFeeds
                        )
                    }
                } catch (e: Exception) {
                    Log.e("NewsSyncWorker", "Sync failed for category $cat", e)
                }
            }
            Log.d("NewsSyncWorker", "Background sync completed successfully")
            return Result.success()
        } catch (e: Exception) {
            Log.e("NewsSyncWorker", "Background sync worker failed", e)
            return Result.retry()
        }
    }
}
