package uk.sume.news.data.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.launch
import org.jsoup.Connection
import org.jsoup.Jsoup
import uk.sume.news.data.local.AppDatabase
import uk.sume.news.data.model.Article
import uk.sume.news.data.model.CustomFeed
import java.util.concurrent.TimeUnit

class NewsRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val articleDao = db.articleDao()
    private val customFeedDao = db.customFeedDao()
    private val parser = RssParser()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val userAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    // Fetch Google News RSS feed
    suspend fun fetchGoogleNews(category: String, language: String, region: String) = withContext(Dispatchers.IO) {
        val topic = category.uppercase()
        val url = if (topic == "FOR YOU" || topic == "LATEST" || topic == "TOP STORIES") {
            "https://news.google.com/rss?hl=$language&gl=$region&ceid=$region:$language"
        } else {
            "https://news.google.com/rss/headlines/section/topic/$topic?hl=$language&gl=$region&ceid=$region:$language"
        }

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val xml = response.body?.string() ?: ""
                    val articles = parser.parse(xml, category)
                    articleDao.clearNonBookmarkedArticlesByCategory(category)
                    articleDao.insertArticles(articles)
                    
                    // Trigger asynchronous thumbnail parsing for the newly fetched articles
                    triggerThumbnailResolution(articles)
                }
            }
        } catch (e: Exception) {
            Log.e("NewsRepository", "Error fetching google news", e)
        }
    }

    // Fetch Custom RSS Feed
    suspend fun fetchCustomFeed(feed: CustomFeed) = withContext(Dispatchers.IO) {
        try {
            var url = feed.url.trim()
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val xml = response.body?.string() ?: ""
                    val articles = parser.parse(xml, feed.category, feed.id)
                    articleDao.clearNonBookmarkedArticlesByCategory(feed.category)
                    articleDao.insertArticles(articles)
                    triggerThumbnailResolution(articles)
                }
            }
        } catch (e: Exception) {
            Log.e("NewsRepository", "Error fetching custom feed ${feed.url}", e)
        }
    }

    // Search news online using Google News RSS search endpoint
    suspend fun searchNewsOnline(query: String, language: String, region: String, category: String = "SEARCH") = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext
        val url = "https://news.google.com/rss/search?q=$query&hl=$language&gl=$region&ceid=$region:$language"
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val xml = response.body?.string() ?: ""
                    val articles = parser.parse(xml, category)
                    articleDao.insertArticles(articles)
                    triggerThumbnailResolution(articles)
                }
            }
        } catch (e: Exception) {
            Log.e("NewsRepository", "Error searching news online", e)
        }
    }

    private fun isGibberishOrPromo(line: String): Boolean {
        val lower = line.lowercase()
        val blacklistedKeywords = listOf(
            "subscribe", "subscription", "sign up", "sign-up", "register for free",
            "read more", "related story:", "related article:", "download the app",
            "follow us", "join our channel", "telegram", "whatsapp", "newsletter",
            "copyright", "all rights reserved", "sph media", "terms of use", "privacy policy",
            "advertisement", "ad blocker", "cookies help us", "enable javascript",
            "you do not have an active", "please log in", "already a subscriber",
            "read also:", "photo:", "image:", "source:", "follow us on", "read the full",
            "get a subscription", "this story is for", "all rights reserved"
        )
        
        for (keyword in blacklistedKeywords) {
            if (lower.contains(keyword)) {
                return true
            }
        }
        
        if (line.length < 25) {
            return true
        }
        
        return false
    }

    // Resolve thumbnails asynchronously for a list of articles in parallel
    private suspend fun triggerThumbnailResolution(articles: List<Article>) {
        withContext(Dispatchers.IO) {
            articles.take(15).forEach { article ->
                if (article.thumbnailUrl == null) {
                    launch {
                        resolveThumbnail(article.link)
                    }
                }
            }
        }
    }

    private fun resolveGoogleNewsUrl(googleUrl: String): String {
        try {
            if (!googleUrl.startsWith("https://news.google.com/")) {
                return googleUrl
            }
            
            // Extract the base64 identifier from path
            val uri = java.net.URI(googleUrl)
            val pathParts = uri.path.split("/").filter { it.isNotEmpty() }
            if (pathParts.size < 2) return googleUrl
            val base64Str = pathParts.last()
            
            // Connect to article redirect page to get parameters
            val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            val redirectDoc = Jsoup.connect(googleUrl)
                .userAgent(userAgent)
                .referrer("https://www.google.com")
                .timeout(6000)
                .get()
            
            val element = redirectDoc.select("[data-n-a-sg]").firstOrNull() ?: return googleUrl
            val signature = element.attr("data-n-a-sg")
            val timestamp = element.attr("data-n-a-ts")
            
            // Construct payload for batchexecute
            val payload = """[[["Fbv4je","[\"garturlreq\",[[\"X\",\"X\",[\"X\",\"X\"],null,null,1,1,\"US:en\",null,1,null,null,null,null,null,0,1],\"X\",\"X\",1,[1,1,1],1,1,null,0,0,null,0],\"$base64Str\",$timestamp,\"$signature\"]",null,"generic"]]]"""
            
            // Send batchexecute POST request
            val postResponse = Jsoup.connect("https://news.google.com/_/DotsSplashUi/data/batchexecute")
                .method(Connection.Method.POST)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .userAgent(userAgent)
                .data("f.req", payload)
                .ignoreContentType(true)
                .timeout(6000)
                .execute()
            
            val responseBody = postResponse.body()
            val match = Regex("""https://[^"\\ ]+""").find(responseBody)
            return match?.value ?: googleUrl
        } catch (e: Exception) {
            Log.e("NewsRepository", "Failed to resolve Google News URL: $googleUrl", e)
            return googleUrl
        }
    }

    // Scraping metadata for og:image
    suspend fun resolveThumbnail(articleLink: String) = withContext(Dispatchers.IO) {
        try {
            val realUrl = resolveGoogleNewsUrl(articleLink)
            
            if (realUrl == articleLink && articleLink.startsWith("https://news.google.com/")) {
                // If resolving failed, store "failed" to prevent infinite loops
                articleDao.updateThumbnailUrl(articleLink, "failed")
                return@withContext
            }

            // Second step: Connect to actual article source page with browser-like headers
            val doc = Jsoup.connect(realUrl)
                .userAgent(userAgent)
                .referrer("https://www.google.com")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cache-Control", "max-age=0")
                .header("Connection", "keep-alive")
                .ignoreHttpErrors(true)
                .timeout(6000)
                .get()

            var imageUrl = doc.select("meta[property=og:image]").attr("content")
            if (imageUrl.isBlank()) {
                imageUrl = doc.select("meta[name=twitter:image]").attr("content")
            }
            if (imageUrl.isBlank()) {
                // Fallback to first large image in article if meta tags are missing
                imageUrl = doc.select("article img, main img").firstOrNull()?.attr("abs:src") ?: ""
            }

            // Exclude Google News logo/branding placeholders
            if (imageUrl.contains("googleusercontent.com") || 
                imageUrl.contains("gstatic.com") || 
                imageUrl.contains("google.com")) {
                imageUrl = ""
            }

            if (imageUrl.isNotBlank()) {
                articleDao.updateThumbnailUrl(articleLink, imageUrl)
            } else {
                // Store empty string to indicate resolution failed, preventing infinite scraper retries
                articleDao.updateThumbnailUrl(articleLink, "failed")
            }
        } catch (e: Exception) {
            Log.e("NewsRepository", "Failed resolving thumbnail for $articleLink", e)
            articleDao.updateThumbnailUrl(articleLink, "failed")
        }
    }

    // Scrapes clean text for Reader mode and TTS player
    suspend fun fetchArticleBody(url: String): String = withContext(Dispatchers.IO) {
        try {
            val realUrl = resolveGoogleNewsUrl(url)

            val doc = Jsoup.connect(realUrl)
                .userAgent(userAgent)
                .referrer("https://www.google.com")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cache-Control", "max-age=0")
                .header("Connection", "keep-alive")
                .ignoreHttpErrors(true)
                .timeout(8000)
                .get()

            // Try to extract readable article content by selecting paragraphs inside article/main or standard div blocks
            val paragraphs = doc.select("article p, main p, .post-content p, .article-content p, .story-body p")
            val rawText = if (paragraphs.isNotEmpty()) {
                paragraphs.map { it.text().trim() }
                    .filter { it.isNotBlank() && !isGibberishOrPromo(it) }
                    .joinToString("\n\n")
            } else {
                doc.select("p").map { it.text().trim() }
                    .filter { it.isNotBlank() && !isGibberishOrPromo(it) }
                    .joinToString("\n\n")
            }

            // Cleanup text (remove extremely short paragraphs or cookie prompts)
            val cleanText = rawText.lines()
                .filter { it.length > 30 } // Filter out UI elements / share button lines
                .joinToString("\n\n")

            if (cleanText.isNotBlank()) {
                cleanText
            } else {
                "Unable to parse article text. Please open in WebView to read the full story."
            }
        } catch (e: Exception) {
            Log.e("NewsRepository", "Failed to fetch article body", e)
            "Failed to load article content due to a connection or formatting issue. Please use In-App WebView."
        }
    }

    // Database flow readers
    fun getArticlesByCategory(category: String): Flow<List<Article>> = articleDao.getArticlesByCategory(category)
    fun getBookmarkedArticles(): Flow<List<Article>> = articleDao.getBookmarkedArticles()
    fun searchArticlesLocal(query: String): Flow<List<Article>> = articleDao.searchArticles(query)
    fun getAllArticles(): Flow<List<Article>> = articleDao.getAllArticles()

    // Bookmark actions
    suspend fun toggleBookmark(articleLink: String, isBookmarked: Boolean) {
        articleDao.updateBookmarkStatus(articleLink, isBookmarked)
    }

    // Custom Feed actions
    fun getCustomFeeds(): Flow<List<CustomFeed>> = customFeedDao.getAllFeeds()
    suspend fun addCustomFeed(feed: CustomFeed) = customFeedDao.insertFeed(feed)
    suspend fun deleteCustomFeed(feed: CustomFeed) = customFeedDao.deleteFeed(feed)
}
