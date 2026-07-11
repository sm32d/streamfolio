package uk.sume.streamfolio.data.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.jsoup.Connection
import org.jsoup.Jsoup
import uk.sume.streamfolio.data.local.AppDatabase
import uk.sume.streamfolio.data.local.PreferencesHelper
import uk.sume.streamfolio.data.model.Article
import uk.sume.streamfolio.data.model.CustomFeed
import java.util.concurrent.TimeUnit

class NewsRepository(private val context: Context) {
    companion object {
        private const val THUMBNAIL_RESOLUTION_BATCH_SIZE = 40
        private const val STRAITS_TIMES_THUMBNAIL_RETRY_BATCH_SIZE = 30
    }

    private val db = AppDatabase.getDatabase(context)
    private val articleDao = db.articleDao()
    private val customFeedDao = db.customFeedDao()
    private val prefs = PreferencesHelper(context)
    private val parser = RssParser()
    private val aiSummaryHelper by lazy { AiHelperFactory.createSummaryHelper(context) }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val userAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    private fun buildBrowserRequest(url: String): Request {
        return Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Connection", "keep-alive")
            .header("Cache-Control", "max-age=0")
            .build()
    }

    // Fetch Default Curated RSS Feeds
    suspend fun fetchDefaultFeeds(
        category: String,
        language: String,
        region: String,
        disabledFeedUrls: Set<String> = emptySet(),
        enabledCrossRegionFeeds: Set<String> = emptySet()
    ) = withContext(Dispatchers.IO) {
        val feedUrls = DefaultFeedsConfig.getFeedsFor(region, category, disabledFeedUrls, enabledCrossRegionFeeds)
        val allArticles = mutableListOf<Article>()
        
        val jobs = feedUrls.map { url ->
            async {
                val feedArticles = mutableListOf<Article>()
                try {
                    val request = buildBrowserRequest(url)
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val xml = response.body?.string() ?: ""
                            val parsed = parser.parse(xml, category, feedUrl = url)
                            feedArticles.addAll(parsed)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NewsRepository", "Error fetching default feed: $url", e)
                }
                feedArticles
            }
        }
        
        val results = jobs.awaitAll()
        for (list in results) {
            allArticles.addAll(list)
        }
        
        insertAndPruneArticles(allArticles, category)
        triggerThumbnailResolution(allArticles)
        triggerStraitsTimesThumbnailRetry(category, allArticles)
        triggerArticleBodyPreScrape(allArticles)
        triggerArticleTagging(allArticles)
    }

    // Fetch multiple Custom RSS Feeds for a single category/tab in parallel
    suspend fun fetchCustomFeeds(feeds: List<CustomFeed>, category: String) = withContext(Dispatchers.IO) {
        val allArticles = mutableListOf<Article>()
        val jobs = feeds.map { feed ->
            async {
                val feedArticles = mutableListOf<Article>()
                try {
                    var url = feed.url.trim()
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://$url"
                    }
                    val request = buildBrowserRequest(url)

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val xml = response.body?.string() ?: ""
                            val parsed = if (parser.isOpml(xml)) {
                                val urls = parser.parseOpmlUrls(xml)
                                val aggregated = mutableListOf<Article>()
                                for (childUrl in urls) {
                                    try {
                                        var formattedChildUrl = childUrl.trim()
                                        if (!formattedChildUrl.startsWith("http://") && !formattedChildUrl.startsWith("https://")) {
                                            formattedChildUrl = "https://$formattedChildUrl"
                                        }
                                        val childRequest = buildBrowserRequest(formattedChildUrl)
                                        client.newCall(childRequest).execute().use { childResponse ->
                                            if (childResponse.isSuccessful) {
                                                val childXml = childResponse.body?.string() ?: ""
                                                val childArticles = parser.parse(childXml, feed.category, feed.id, feedUrl = formattedChildUrl)
                                                aggregated.addAll(childArticles)
                                            }
                                        }
                                    } catch (childEx: Exception) {
                                        Log.e("NewsRepository", "Error fetching OPML child feed $childUrl", childEx)
                                    }
                                }
                                aggregated
                            } else {
                                parser.parse(xml, feed.category, feed.id, feedUrl = url)
                            }
                            feedArticles.addAll(parsed)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NewsRepository", "Error fetching custom feed ${feed.url}", e)
                }
                feedArticles
            }
        }
        val results = jobs.awaitAll()
        for (list in results) {
            allArticles.addAll(list)
        }
        
        insertAndPruneArticles(allArticles, category)
        triggerThumbnailResolution(allArticles)
        triggerStraitsTimesThumbnailRetry(category, allArticles)
        triggerArticleBodyPreScrape(allArticles)
        triggerArticleTagging(allArticles)
    }

    private suspend fun insertAndPruneArticles(allArticles: List<Article>, category: String) {
        articleDao.insertOrUpdateArticles(allArticles)
        val historyDays = prefs.cacheHistoryDays
        if (historyDays < 36500) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -historyDays)
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val expiryDate = sdf.format(cal.time)
            articleDao.pruneOldArticles(expiryDate)
        }
    }

    // Search news online by querying active default and custom feeds, and filtering results locally
    suspend fun searchNewsOnline(
        query: String,
        language: String,
        region: String,
        category: String = "SEARCH",
        activeCategories: Set<String>,
        customFeeds: List<CustomFeed>,
        disabledFeedUrls: Set<String> = emptySet(),
        enabledCrossRegionFeeds: Set<String> = emptySet()
    ) = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext
        val allArticles = mutableListOf<Article>()
        val urlsToSearch = mutableListOf<Pair<String, String>>()
        
        for (cat in activeCategories) {
            val feeds = DefaultFeedsConfig.getFeedsFor(region, cat, disabledFeedUrls, enabledCrossRegionFeeds)
            for (url in feeds) {
                urlsToSearch.add(Pair(url, cat))
            }
        }
        
        for (feed in customFeeds) {
            urlsToSearch.add(Pair(feed.url, feed.category))
        }
        
        val distinctUrls = urlsToSearch.distinctBy { it.first }
        val jobs = distinctUrls.map { (url, cat) ->
            async {
                val matchingArticles = mutableListOf<Article>()
                try {
                    var formattedUrl = url.trim()
                    if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                        formattedUrl = "https://$formattedUrl"
                    }
                    val request = buildBrowserRequest(formattedUrl)
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val xml = response.body?.string() ?: ""
                            val parsed = if (parser.isOpml(xml)) {
                                val childUrls = parser.parseOpmlUrls(xml)
                                val aggregated = mutableListOf<Article>()
                                for (childUrl in childUrls) {
                                    try {
                                        var fc = childUrl.trim()
                                        if (!fc.startsWith("http://") && !fc.startsWith("https://")) fc = "https://$fc"
                                        val childReq = buildBrowserRequest(fc)
                                        client.newCall(childReq).execute().use { childResp ->
                                            if (childResp.isSuccessful) {
                                                val childXml = childResp.body?.string() ?: ""
                                                val childParsed = parser.parse(childXml, cat, feedUrl = fc)
                                                aggregated.addAll(childParsed)
                                            }
                                        }
                                    } catch (ex: Exception) {
                                        // Ignore child failures
                                    }
                                }
                                aggregated
                            } else {
                                parser.parse(xml, cat, feedUrl = formattedUrl)
                            }
                            
                            val cleanQuery = query.replace("\"", "").lowercase()
                            val filtered = parsed.filter {
                                it.title.lowercase().contains(cleanQuery) || 
                                it.description.lowercase().contains(cleanQuery) ||
                                it.sourceName.lowercase().contains(cleanQuery)
                            }
                            matchingArticles.addAll(filtered)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NewsRepository", "Error searching feed: $url", e)
                }
                matchingArticles
            }
        }
        
        val results = jobs.awaitAll()
        for (list in results) {
            allArticles.addAll(list)
        }
        
        articleDao.clearNonBookmarkedArticlesByCategory(category)
        articleDao.insertOrUpdateArticles(allArticles)
        triggerThumbnailResolution(allArticles)
        triggerArticleBodyPreScrape(allArticles)
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
            articles
                .filter { it.thumbnailUrl.isNullOrBlank() }
                .take(THUMBNAIL_RESOLUTION_BATCH_SIZE)
                .forEach { article ->
                    launch {
                        resolveThumbnail(article.link)
                    }
                }
        }
    }

    private fun isStraitsTimesArticle(article: Article): Boolean {
        return article.sourceName.contains("Straits Times", ignoreCase = true) ||
            article.sourceUrl.contains("straitstimes.com", ignoreCase = true) ||
            article.link.contains("straitstimes.com", ignoreCase = true)
    }

    private suspend fun triggerStraitsTimesFreshArticleRetry(fetchedArticles: List<Article>) {
        withContext(Dispatchers.IO) {
            fetchedArticles
                .filter { isStraitsTimesArticle(it) && it.thumbnailUrl.isNullOrBlank() }
                .take(STRAITS_TIMES_THUMBNAIL_RETRY_BATCH_SIZE)
                .forEach { article ->
                    launch {
                        resolveThumbnail(article.link)
                    }
            }
        }
    }

    private suspend fun triggerStraitsTimesThumbnailRetry(category: String, fetchedArticles: List<Article>) {
        val hasStraitsTimesInRefresh = fetchedArticles.any { article -> isStraitsTimesArticle(article) }
        if (!hasStraitsTimesInRefresh) return

        triggerStraitsTimesFreshArticleRetry(fetchedArticles)

        withContext(Dispatchers.IO) {
            val retryCandidates = articleDao.getStraitsTimesArticlesNeedingThumbnailRetry(
                category,
                STRAITS_TIMES_THUMBNAIL_RETRY_BATCH_SIZE
            )
            retryCandidates.forEach { article ->
                launch {
                    resolveThumbnail(article.link)
                }
            }
        }
    }

    // Pre-scrape full text for the top 10 articles in parallel in the background
    private suspend fun triggerArticleBodyPreScrape(articles: List<Article>) {
        withContext(Dispatchers.IO) {
            articles.take(10).forEach { article ->
                val existing = articleDao.getArticleByLink(article.link)
                if (existing == null || existing.fullText == null) {
                    launch {
                        try {
                            val body = fetchArticleBody(article.link)
                            if (body.isNotBlank() && !body.startsWith("Failed to load") && !body.startsWith("Unable to parse")) {
                                articleDao.updateFullText(article.link, body)
                            }
                        } catch (e: Exception) {
                            Log.e("NewsRepository", "Failed pre-scraping for ${article.link}", e)
                        }
                    }
                }
            }
        }
    }

    private suspend fun triggerArticleTagging(articles: List<Article>) {
        if (!prefs.isAiEnabled || !prefs.isSmartTagsEnabled) return
        withContext(Dispatchers.IO) {
            val untagged = articleDao.getArticlesWithoutTags(20)
            if (untagged.isEmpty()) return@withContext

            untagged.forEach { article ->
                launch {
                    try {
                        var aiSummary = ""
                        try {
                            val helper = aiSummaryHelper
                            if (helper != null) {
                                val status = helper.checkFeatureStatus()
                                if (status != 0 && status != 1 && status != 2) {
                                    aiSummary = helper.summarizeText(article.title + "\n" + article.description)
                                }
                            }
                        } catch (e: Throwable) {
                            Log.d("NewsRepository", "Nano summarizer unavailable for tagging. Using text-parsing fallback.")
                        }

                        val tags = extractTagsFromContent(article.title, article.description, aiSummary, article.category ?: "")
                        if (tags.isNotBlank()) {
                            articleDao.updateTags(article.link, tags)
                        } else {
                            articleDao.updateTags(article.link, "")
                        }
                    } catch (e: Exception) {
                        Log.e("NewsRepository", "Failed tagging for ${article.link}", e)
                    }
                }
            }
        }
    }

    private fun extractTagsFromContent(title: String, description: String, aiSummary: String, category: String): String {
        val content = "$title $description $aiSummary".lowercase()
        val tags = mutableSetOf<String>()

        val keywordToTagMap = mapOf(
            "artificial intelligence" to "#AI",
            "chatgpt" to "#AI",
            "machine learning" to "#AI",
            "gemini" to "#AI",
            "deep learning" to "#AI",
            "neural network" to "#AI",
            "inflation" to "#Inflation",
            "interest rate" to "#Inflation",
            "federal reserve" to "#Inflation",
            "cpi" to "#Inflation",
            "economy" to "#Economy",
            "recession" to "#Economy",
            "gdp" to "#Economy",
            "stock market" to "#Markets",
            "nasdaq" to "#Markets",
            "s&p 500" to "#Markets",
            "dow jones" to "#Markets",
            "wall street" to "#Markets",
            "investing" to "#Markets",
            "market cap" to "#Markets",
            "shares slide" to "#Markets",
            "shares rally" to "#Markets",
            "bond market" to "#Markets",
            "football" to "#Sports",
            "soccer" to "#Sports",
            "basketball" to "#Sports",
            "nba" to "#Sports",
            "world cup" to "#Sports",
            "olympics" to "#Sports",
            "super bowl" to "#Sports",
            "championship" to "#Sports",
            "bitcoin" to "#Crypto",
            "ethereum" to "#Crypto",
            "cryptocurrency" to "#Crypto",
            "crypto" to "#Crypto",
            "blockchain" to "#Crypto",
            "presidential election" to "#Politics",
            "senate" to "#Politics",
            "political party" to "#Politics",
            "parliament" to "#Politics",
            "legislature" to "#Politics",
            "white house" to "#Politics",
            "capitol hill" to "#Politics",
            "congress" to "#Politics",
            "democrat" to "#Politics",
            "republican" to "#Politics",
            "climate change" to "#Climate",
            "global warming" to "#Climate",
            "greenhouse gas" to "#Climate",
            "greenhouse gases" to "#Climate",
            "carbon emissions" to "#Climate",
            "renewable energy" to "#Climate",
            "cancer" to "#Health",
            "fda" to "#Health",
            "vaccine" to "#Health",
            "medicine" to "#Health",
            "disease" to "#Health",
            "covid" to "#Health",
            "virus" to "#Health",
            "outer space" to "#Space",
            "astronomy" to "#Space",
            "orbit" to "#Space",
            "rocket launch" to "#Space",
            "nasa" to "#Space",
            "spacex" to "#Space",
            "mars rover" to "#Space",
            "space station" to "#Space",
            "entertainment" to "#Entertainment",
            "movie" to "#Entertainment",
            "music" to "#Entertainment",
            "hollywood" to "#Entertainment",
            "actor" to "#Entertainment",
            "singer" to "#Entertainment",
            "concert" to "#Entertainment",
            "film" to "#Entertainment",
            "technology" to "#Tech",
            "software" to "#Tech",
            "hardware" to "#Tech",
            "gadget" to "#Tech",
            "smartphone" to "#Tech",
            "apple" to "#Tech",
            "google" to "#Tech",
            "microsoft" to "#Tech",
            "science" to "#Science",
            "physics" to "#Science",
            "biology" to "#Science",
            "chemistry" to "#Science",
            "research" to "#Science",
            "business" to "#Business",
            "company" to "#Business",
            "revenue" to "#Business",
            "startup" to "#Business",
            "ceo" to "#Business",
            "china" to "#World",
            "russia" to "#World",
            "europe" to "#World",
            "ukraine" to "#World",
            "middle east" to "#World",
            "nato" to "#World",
            "global" to "#World",
            "military" to "#Defense",
            "defense" to "#Defense",
            "army" to "#Defense",
            "war" to "#Defense",
            "missile" to "#Defense",
            "travel" to "#Travel",
            "tourism" to "#Travel",
            "flight" to "#Travel",
            "hotel" to "#Travel",
            "fashion" to "#Lifestyle",
            "wellness" to "#Lifestyle",
            "fitness" to "#Lifestyle",
            "electric vehicle" to "#Automotive",
            "electric vehicles" to "#Automotive",
            "tesla" to "#Automotive",
            "autonomous driving" to "#Automotive",
            "concept car" to "#Automotive",
            "automaker" to "#Automotive",
            "automakers" to "#Automotive",
            "electric cars" to "#Automotive",
            "hybrid vehicles" to "#Automotive",
            "supercar" to "#Automotive",
            "supercars" to "#Automotive",
            "auto industry" to "#Automotive",
            "car manufacturer" to "#Automotive",
            "car manufacturers" to "#Automotive",
            "ev charging" to "#Automotive"
        )

        for ((keyword, tag) in keywordToTagMap) {
            if (content.contains(keyword)) {
                tags.add(tag)
            }
        }

        // If no high-level keywords match, fallback to category mapping so every story is groupable
        if (tags.isEmpty() && category.isNotBlank()) {
            val categoryTagMap = mapOf(
                "top stories" to "#TopStories",
                "world" to "#World",
                "business" to "#Business",
                "technology" to "#Tech",
                "science" to "#Science",
                "sports" to "#Sports",
                "health" to "#Health",
                "entertainment" to "#Entertainment"
            )
            val normalizedCat = category.lowercase().trim()
            val fallbackTag = categoryTagMap[normalizedCat] ?: "#${category.replace(Regex("\\s+"), "")}"
            tags.add(fallbackTag)
        }

        return tags.take(4).joinToString(", ")
    }

    suspend fun getArticleByLink(link: String): Article? = articleDao.getArticleByLink(link)

    suspend fun resolveThumbnailIfNeeded(link: String) = withContext(Dispatchers.IO) {
        val article = articleDao.getArticleByLink(link) ?: return@withContext
        if (article.thumbnailUrl.isNullOrBlank() || article.thumbnailUrl == "failed") {
            resolveThumbnail(link)
        }
    }

    suspend fun insertArticle(article: Article) = withContext(Dispatchers.IO) {
        articleDao.insertOrUpdateArticles(listOf(article))
    }

    suspend fun updateFullText(link: String, fullText: String?) = articleDao.updateFullText(link, fullText)

    fun resolveGoogleNewsUrl(googleUrl: String): String {
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
                imageUrl = extractImageFromStructuredData(doc.html())
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

    private fun extractImageFromStructuredData(html: String): String {
        val patterns = listOf(
            Regex(""""image"\s*:\s*\{[\s\S]*?"url"\s*:\s*"([^"]+)""""),
            Regex("""\\"image\\"\s*:\s*\{[\s\S]*?\\"url\\"\s*:\s*\\"([^\\]+)\\""""),
            Regex("""https://cassette\.sphdigital\.com\.sg/image/[^"\\\s<]+""")
        )

        for (pattern in patterns) {
            val match = pattern.find(html) ?: continue
            val value = match.groups[1]?.value ?: match.value
            val normalized = value
                .replace("\\/", "/")
                .trim()
                .removeSuffix("\\")
            if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
                return normalized
            }
        }
        return ""
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
    suspend fun getAllCustomFeedsSync(): List<CustomFeed> = customFeedDao.getAllFeedsSync()
    suspend fun addCustomFeed(feed: CustomFeed) = customFeedDao.insertFeed(feed)
    suspend fun addCustomFeeds(feeds: List<CustomFeed>) = customFeedDao.insertFeeds(feeds)
    suspend fun deleteCustomFeed(feed: CustomFeed) = customFeedDao.deleteFeed(feed)
    suspend fun deleteAllCustomFeeds() = customFeedDao.deleteAllFeeds()

    suspend fun deleteArticlesForFeed(sourceUrl: String) = withContext(Dispatchers.IO) {
        articleDao.deleteArticlesBySourceUrl(sourceUrl)
    }

    suspend fun fetchSingleFeed(url: String, category: String) = withContext(Dispatchers.IO) {
        val allArticles = mutableListOf<Article>()
        try {
            val request = buildBrowserRequest(url)
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val xml = response.body?.string() ?: ""
                    val parsed = parser.parse(xml, category, feedUrl = url)
                    allArticles.addAll(parsed)
                }
            }
        } catch (e: Exception) {
            Log.e("NewsRepository", "Error fetching single feed: $url", e)
        }
        if (allArticles.isNotEmpty()) {
            insertAndPruneArticles(allArticles, category)
        }
    }

    suspend fun clearAllTags() = withContext(Dispatchers.IO) {
        articleDao.clearAllTags()
    }

    suspend fun updateAiSummary(link: String, aiSummary: String?) = withContext(Dispatchers.IO) {
        articleDao.updateAiSummary(link, aiSummary)
    }

    suspend fun updateTranslation(
        link: String,
        translatedTitle: String?,
        translatedBody: String?,
        translatedLanguage: String?,
        detectedLanguage: String?
    ) = withContext(Dispatchers.IO) {
        articleDao.updateTranslation(link, translatedTitle, translatedBody, translatedLanguage, detectedLanguage)
    }

    suspend fun updateDetectedLanguage(link: String, detectedLanguage: String?) = withContext(Dispatchers.IO) {
        articleDao.updateDetectedLanguage(link, detectedLanguage)
    }

    suspend fun updateReadStatus(link: String, isRead: Boolean) = withContext(Dispatchers.IO) {
        articleDao.updateReadStatus(link, isRead)
    }
}
