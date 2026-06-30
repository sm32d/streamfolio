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
                val updated = article.copy(isBookmarked = existing.isBookmarked)
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

    @Query("SELECT * FROM articles ORDER BY pubDate DESC LIMIT 50")
    fun getAllArticles(): Flow<List<Article>>

    @Query("DELETE FROM articles WHERE category = :category AND isBookmarked = 0")
    suspend fun clearNonBookmarkedArticlesByCategory(category: String)

    @Query("DELETE FROM articles WHERE isBookmarked = 0 AND pubDate < :expiryDate")
    suspend fun pruneOldArticles(expiryDate: String)

    @Query("DELETE FROM articles WHERE sourceUrl = :sourceUrl AND isBookmarked = 0")
    suspend fun deleteArticlesBySourceUrl(sourceUrl: String)
}

@Dao
interface CustomFeedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: CustomFeed)

    @Delete
    suspend fun deleteFeed(feed: CustomFeed)

    @Query("SELECT * FROM custom_feeds ORDER BY id DESC")
    fun getAllFeeds(): Flow<List<CustomFeed>>
}
