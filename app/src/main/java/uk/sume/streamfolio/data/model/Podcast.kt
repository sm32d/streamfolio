package uk.sume.streamfolio.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "podcast_subscriptions")
data class PodcastSubscription(
    @PrimaryKey
    val feedId: Int,
    val title: String,
    val author: String,
    val description: String,
    val feedUrl: String,
    val imageUri: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "podcast_episodes")
data class PodcastEpisode(
    @PrimaryKey
    val episodeId: Long,
    val feedId: Int,
    val title: String,
    val description: String,
    val audioUrl: String,
    val pubDate: Long,
    val durationSeconds: Int,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val playbackPositionMs: Long = 0L,
    val isBookmarked: Boolean = false,
    val isRead: Boolean = false,
    val transcriptUrl: String? = null,
    val transcriptType: String? = null
)
