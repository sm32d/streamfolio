package uk.sume.streamfolio.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey
    val link: String,
    val title: String,
    val description: String,
    val pubDate: String,
    val sourceName: String,
    val sourceUrl: String,
    val category: String,
    val thumbnailUrl: String? = null,
    val isBookmarked: Boolean = false,
    val customFeedId: Int? = null,
    val fullText: String? = null
)
