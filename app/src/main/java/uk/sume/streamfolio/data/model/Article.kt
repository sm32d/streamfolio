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
    val fullText: String? = null,
    val tags: String? = null,
    val aiSummary: String? = null,
    val translatedTitle: String? = null,
    val translatedBody: String? = null,
    val translatedLanguage: String? = null,
    val detectedLanguage: String? = null,
    val isRead: Boolean = false,
    val groupId: String? = null
)

data class ArticleGroup(
    val primary: Article,
    val secondary: List<Article> = emptyList()
)
