package uk.sume.streamfolio.data.network

import android.content.Context
import kotlinx.coroutines.flow.Flow
import uk.sume.streamfolio.data.local.AppDatabase
import uk.sume.streamfolio.data.model.PodcastEpisode
import uk.sume.streamfolio.data.model.PodcastSubscription

class PodcastRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val podcastDao = db.podcastDao()
    private val api = PodcastIndexApi()

    fun getAllSubscriptions(): Flow<List<PodcastSubscription>> = podcastDao.getAllSubscriptions()

    suspend fun getSubscriptionById(feedId: Int): PodcastSubscription? = podcastDao.getSubscriptionById(feedId)

    suspend fun subscribe(podcast: PodcastSubscription) {
        podcastDao.insertSubscription(podcast)
    }

    suspend fun unsubscribe(podcast: PodcastSubscription) {
        podcastDao.deleteSubscription(podcast)
    }

    fun getEpisodesForPodcast(feedId: Int): Flow<List<PodcastEpisode>> = podcastDao.getEpisodesForPodcast(feedId)

    fun getBookmarkedEpisodes(): Flow<List<PodcastEpisode>> = podcastDao.getBookmarkedEpisodes()

    fun getDownloadedEpisodes(): Flow<List<PodcastEpisode>> = podcastDao.getDownloadedEpisodes()

    suspend fun getDownloadedEpisodesList(): List<PodcastEpisode> = podcastDao.getDownloadedEpisodesList()

    suspend fun updatePlaybackPosition(episodeId: Long, position: Long) {
        podcastDao.updatePlaybackPosition(episodeId, position)
    }

    suspend fun updateBookmarkStatus(episodeId: Long, isBookmarked: Boolean) {
        podcastDao.updateBookmarkStatus(episodeId, isBookmarked)
    }

    suspend fun updateReadStatus(episodeId: Long, isRead: Boolean) {
        podcastDao.updateReadStatus(episodeId, isRead)
    }

    suspend fun updateDownloadStatus(episodeId: Long, isDownloaded: Boolean, localFilePath: String?) {
        podcastDao.updateDownloadStatus(episodeId, isDownloaded, localFilePath)
    }

    suspend fun clearAllDownloads() {
        podcastDao.clearAllDownloads()
    }

    suspend fun searchPodcasts(query: String, key: String, secret: String): List<PodcastSubscription> {
        return api.searchPodcasts(query, key, secret)
    }

    suspend fun fetchAndCacheEpisodes(feedId: Int, key: String, secret: String) {
        val episodes = api.getEpisodes(feedId, key, secret)
        if (episodes.isNotEmpty()) {
            podcastDao.insertOrUpdateEpisodes(episodes)
        }
    }
    
    suspend fun getEpisodeById(episodeId: Long): PodcastEpisode? {
        return podcastDao.getEpisodeById(episodeId)
    }
}
