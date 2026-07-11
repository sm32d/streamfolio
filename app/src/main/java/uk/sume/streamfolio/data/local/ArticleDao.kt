package uk.sume.streamfolio.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import uk.sume.streamfolio.data.model.Article
import uk.sume.streamfolio.data.model.CustomFeed

@Dao
interface ArticleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticle(article: Article)

    @Update
    suspend fun updateArticle(article: Article)

    @Query("SELECT * FROM articles WHERE link = :link")
    suspend fun getArticleByLink(link: String): Article?

    @Transaction
    suspend fun insertOrUpdateArticles(articles: List<Article>) {
        for (article in articles) {
            val existing = getArticleByLink(article.link)
            if (existing != null) {
                val updated = article.copy(
                    isBookmarked = existing.isBookmarked,
                    isRead = existing.isRead,
                    fullText = existing.fullText,
                    aiSummary = existing.aiSummary,
                    translatedTitle = existing.translatedTitle,
                    translatedBody = existing.translatedBody,
                    translatedLanguage = existing.translatedLanguage,
                    detectedLanguage = existing.detectedLanguage
                )
                updateArticle(updated)
            } else {
                insertArticle(article)
            }
        }
    }

    @Query("UPDATE articles SET isBookmarked = :isBookmarked WHERE link = :link")
    suspend fun updateBookmarkStatus(link: String, isBookmarked: Boolean)

    @Query("UPDATE articles SET thumbnailUrl = :thumbnailUrl WHERE link = :link")
    suspend fun updateThumbnailUrl(link: String, thumbnailUrl: String?)

    @Query("SELECT * FROM articles WHERE category = :category ORDER BY pubDate DESC")
    fun getArticlesByCategory(category: String): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY pubDate DESC")
    fun getBookmarkedArticles(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY pubDate DESC")
    fun searchArticles(query: String): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE thumbnailUrl IS NULL ORDER BY pubDate DESC LIMIT :limit")
    suspend fun getArticlesWithoutThumbnail(limit: Int): List<Article>

    @Query(
        """
        SELECT * FROM articles
        WHERE category = :category
          AND (
            LOWER(sourceName) LIKE '%straits times%'
            OR LOWER(sourceUrl) LIKE '%straitstimes.com%'
          )
          AND (
            thumbnailUrl IS NULL
            OR thumbnailUrl = ''
            OR thumbnailUrl = 'failed'
          )
        ORDER BY pubDate DESC
        LIMIT :limit
        """
    )
    suspend fun getStraitsTimesArticlesNeedingThumbnailRetry(category: String, limit: Int): List<Article>

    @Query("UPDATE articles SET fullText = :fullText WHERE link = :link")
    suspend fun updateFullText(link: String, fullText: String?)

    @Query("UPDATE articles SET aiSummary = :aiSummary WHERE link = :link")
    suspend fun updateAiSummary(link: String, aiSummary: String?)

    @Query("UPDATE articles SET translatedTitle = :translatedTitle, translatedBody = :translatedBody, translatedLanguage = :translatedLanguage, detectedLanguage = :detectedLanguage WHERE link = :link")
    suspend fun updateTranslation(link: String, translatedTitle: String?, translatedBody: String?, translatedLanguage: String?, detectedLanguage: String?)

    @Query("UPDATE articles SET detectedLanguage = :detectedLanguage WHERE link = :link")
    suspend fun updateDetectedLanguage(link: String, detectedLanguage: String?)

    @Query("UPDATE articles SET isRead = :isRead WHERE link = :link")
    suspend fun updateReadStatus(link: String, isRead: Boolean)

    @Query("SELECT * FROM articles WHERE fullText IS NULL AND category != 'bookmarks' ORDER BY pubDate DESC LIMIT :limit")
    suspend fun getArticlesWithoutFullText(limit: Int): List<Article>

    @Query("UPDATE articles SET tags = :tags WHERE link = :link")
    suspend fun updateTags(link: String, tags: String?)

    @Query("UPDATE articles SET tags = NULL")
    suspend fun clearAllTags()

    @Query("SELECT * FROM articles WHERE tags IS NULL AND isBookmarked = 0 ORDER BY pubDate DESC LIMIT :limit")
    suspend fun getArticlesWithoutTags(limit: Int): List<Article>

    @Query("SELECT * FROM articles ORDER BY pubDate DESC LIMIT 50")
    fun getAllArticles(): Flow<List<Article>>

    @Query("DELETE FROM articles WHERE category = :category AND isBookmarked = 0")
    suspend fun clearNonBookmarkedArticlesByCategory(category: String)

    @Query("DELETE FROM articles WHERE isBookmarked = 0 AND pubDate < :expiryDate")
    suspend fun pruneOldArticles(expiryDate: String)

    @Query("DELETE FROM articles WHERE sourceUrl = :sourceUrl AND isBookmarked = 0")
    suspend fun deleteArticlesBySourceUrl(sourceUrl: String)

    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY pubDate DESC LIMIT 50")
    suspend fun getBookmarkedArticlesSync(): List<Article>

    @Query("SELECT * FROM articles ORDER BY pubDate DESC LIMIT 50")
    suspend fun getLatestArticlesSync(): List<Article>

    @Query("SELECT * FROM articles WHERE category = :category ORDER BY pubDate DESC LIMIT 50")
    suspend fun getArticlesByCategorySync(category: String): List<Article>
}

@Dao
interface CustomFeedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: CustomFeed)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeeds(feeds: List<CustomFeed>)

    @Delete
    suspend fun deleteFeed(feed: CustomFeed)

    @Query("DELETE FROM custom_feeds")
    suspend fun deleteAllFeeds()

    @Query("SELECT * FROM custom_feeds ORDER BY id DESC")
    fun getAllFeeds(): Flow<List<CustomFeed>>

    @Query("SELECT * FROM custom_feeds ORDER BY id DESC")
    suspend fun getAllFeedsSync(): List<CustomFeed>
}
