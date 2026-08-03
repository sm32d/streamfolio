package uk.sume.streamfolio.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import uk.sume.streamfolio.data.model.Article
import uk.sume.streamfolio.data.model.ArticleBookmarkListCrossRef
import uk.sume.streamfolio.data.model.BookmarkList

@Dao
interface BookmarkListDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertList(list: BookmarkList): Long

    @Query("SELECT * FROM bookmark_lists ORDER BY isDefault DESC, createdAt ASC")
    fun getAllLists(): Flow<List<BookmarkList>>

    @Query("SELECT * FROM bookmark_lists WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultList(): BookmarkList?

    @Query("SELECT * FROM bookmark_lists WHERE id = :listId LIMIT 1")
    suspend fun getListById(listId: Long): BookmarkList?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: ArticleBookmarkListCrossRef)

    @Query("DELETE FROM article_bookmark_lists WHERE articleLink = :articleLink AND listId = :listId")
    suspend fun deleteCrossRef(articleLink: String, listId: Long)

    @Query("DELETE FROM article_bookmark_lists WHERE articleLink = :articleLink")
    suspend fun deleteAllCrossRefsForArticle(articleLink: String)

    @Query("SELECT listId FROM article_bookmark_lists WHERE articleLink = :articleLink")
    fun getListIdsForArticle(articleLink: String): Flow<List<Long>>

    @Query("SELECT listId FROM article_bookmark_lists WHERE articleLink = :articleLink")
    suspend fun getListIdsForArticleSync(articleLink: String): List<Long>

    @Query("""
        SELECT a.* FROM articles a 
        INNER JOIN article_bookmark_lists crossRef ON a.link = crossRef.articleLink 
        WHERE crossRef.listId = :listId 
        ORDER BY a.pubDate DESC
    """)
    fun getArticlesForList(listId: Long): Flow<List<Article>>

    @Query("SELECT COUNT(*) FROM article_bookmark_lists WHERE listId = :listId")
    fun getArticleCountForList(listId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM article_bookmark_lists WHERE articleLink = :articleLink")
    suspend fun getArticleListAssociationCount(articleLink: String): Int

    @Query("SELECT a.thumbnailUrl FROM articles a INNER JOIN article_bookmark_lists crossRef ON a.link = crossRef.articleLink WHERE crossRef.listId = :listId AND a.thumbnailUrl IS NOT NULL AND a.thumbnailUrl != '' AND a.thumbnailUrl != 'failed' ORDER BY a.pubDate DESC LIMIT 1")
    fun getPreviewThumbnailForList(listId: Long): Flow<String?>

    @Query("DELETE FROM bookmark_lists WHERE id = :listId AND isDefault = 0")
    suspend fun deleteList(listId: Long)

    @Query("DELETE FROM article_bookmark_lists WHERE listId = :listId")
    suspend fun deleteCrossRefsForList(listId: Long)

    @Query("UPDATE OR IGNORE article_bookmark_lists SET listId = :toListId WHERE listId = :fromListId")
    suspend fun reassignListArticles(fromListId: Long, toListId: Long)

    @Query("SELECT * FROM bookmark_lists")
    suspend fun getAllListsSync(): List<BookmarkList>

    @Query("SELECT * FROM article_bookmark_lists")
    suspend fun getAllCrossRefsSync(): List<ArticleBookmarkListCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLists(lists: List<BookmarkList>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<ArticleBookmarkListCrossRef>)

    @Query("DELETE FROM bookmark_lists WHERE isDefault = 0")
    suspend fun deleteAllCustomLists()

    @Query("DELETE FROM article_bookmark_lists")
    suspend fun deleteAllCrossRefs()
}
