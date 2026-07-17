package uk.sume.streamfolio.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import uk.sume.streamfolio.data.model.PodcastEpisode
import uk.sume.streamfolio.data.model.PodcastSubscription

@Dao
interface PodcastDao {

    // --- Subscription Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(podcast: PodcastSubscription)

    @Delete
    suspend fun deleteSubscription(podcast: PodcastSubscription)

    @Query("SELECT * FROM podcast_subscriptions ORDER BY title ASC")
    fun getAllSubscriptions(): Flow<List<PodcastSubscription>>

    @Query("SELECT * FROM podcast_subscriptions WHERE feedId = :feedId")
    suspend fun getSubscriptionById(feedId: Int): PodcastSubscription?


    // --- Episode Operations ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEpisode(episode: PodcastEpisode)

    @Update
    suspend fun updateEpisode(episode: PodcastEpisode)

    @Query("SELECT * FROM podcast_episodes WHERE episodeId = :episodeId")
    suspend fun getEpisodeById(episodeId: Long): PodcastEpisode?

    @Transaction
    suspend fun insertOrUpdateEpisodes(episodes: List<PodcastEpisode>) {
        for (episode in episodes) {
            val existing = getEpisodeById(episode.episodeId)
            if (existing != null) {
                val updated = episode.copy(
                    isDownloaded = existing.isDownloaded,
                    localFilePath = existing.localFilePath,
                    playbackPositionMs = existing.playbackPositionMs,
                    isBookmarked = existing.isBookmarked,
                    isRead = existing.isRead
                )
                updateEpisode(updated)
            } else {
                insertEpisode(episode)
            }
        }
    }

    @Query("SELECT * FROM podcast_episodes WHERE feedId = :feedId ORDER BY pubDate DESC")
    fun getEpisodesForPodcast(feedId: Int): Flow<List<PodcastEpisode>>

    @Query("SELECT * FROM podcast_episodes WHERE isBookmarked = 1 ORDER BY pubDate DESC")
    fun getBookmarkedEpisodes(): Flow<List<PodcastEpisode>>

    @Query("SELECT * FROM podcast_episodes WHERE isDownloaded = 1")
    fun getDownloadedEpisodes(): Flow<List<PodcastEpisode>>

    @Query("SELECT * FROM podcast_episodes WHERE isDownloaded = 1")
    suspend fun getDownloadedEpisodesList(): List<PodcastEpisode>

    @Query("UPDATE podcast_episodes SET isDownloaded = :isDownloaded, localFilePath = :localFilePath WHERE episodeId = :episodeId")
    suspend fun updateDownloadStatus(episodeId: Long, isDownloaded: Boolean, localFilePath: String?)

    @Query("UPDATE podcast_episodes SET playbackPositionMs = :position WHERE episodeId = :episodeId")
    suspend fun updatePlaybackPosition(episodeId: Long, position: Long)

    @Query("UPDATE podcast_episodes SET isBookmarked = :isBookmarked WHERE episodeId = :episodeId")
    suspend fun updateBookmarkStatus(episodeId: Long, isBookmarked: Boolean)

    @Query("UPDATE podcast_episodes SET isRead = :isRead WHERE episodeId = :episodeId")
    suspend fun updateReadStatus(episodeId: Long, isRead: Boolean)

    @Query("UPDATE podcast_episodes SET isDownloaded = 0, localFilePath = null")
    suspend fun clearAllDownloads()
}
