package uk.sume.streamfolio.util

import android.content.Context
import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import uk.sume.streamfolio.data.local.PreferencesHelper
import uk.sume.streamfolio.data.model.Article
import uk.sume.streamfolio.data.model.ArticleBookmarkListCrossRef
import uk.sume.streamfolio.data.model.BookmarkList
import uk.sume.streamfolio.data.model.CustomFeed
import uk.sume.streamfolio.data.network.LinkOnlySources

object BackupHelper {

    fun generateBackupJson(
        context: Context,
        customFeeds: List<CustomFeed>,
        articles: List<Article>,
        bookmarkLists: List<BookmarkList> = emptyList(),
        crossRefs: List<ArticleBookmarkListCrossRef> = emptyList()
    ): String {
        val prefs = PreferencesHelper(context)
        val backupObj = JSONObject()
        backupObj.put("version", 2)

        val prefsObj = JSONObject().apply {
            put("language", prefs.language)
            put("region", prefs.region)
            put("selected_categories", JSONArray(prefs.selectedCategories.toList()))
            put("is_default_feeds_enabled", prefs.isDefaultFeedsEnabled)
            put("category_order", JSONArray(prefs.categoryOrder))
            put("disabled_feed_urls", JSONArray(prefs.disabledFeedUrls.toList()))
            put("enabled_cross_region_feeds", JSONArray(prefs.enabledCrossRegionFeeds.toList()))
            put("cache_history_days", prefs.cacheHistoryDays)
            put("reader_font_family", prefs.readerFontFamily)
            put("reader_font_size", prefs.readerFontSize.toDouble())
            put("reader_line_spacing", prefs.readerLineSpacing.toDouble())
            put("is_ai_enabled", prefs.isAiEnabled)
            put("is_translation_enabled", prefs.isTranslationEnabled)
            put("is_summary_enabled", prefs.isSummaryEnabled)
            put("is_smart_tags_enabled", prefs.isSmartTagsEnabled)
            put("has_seen_ai_spotlight", prefs.hasSeenAiSpotlight)
            put("swipe_left_action", prefs.swipeLeftAction)
            put("swipe_right_action", prefs.swipeRightAction)
            put("tts_speech_rate", prefs.ttsSpeechRate.toDouble())
            put("use_dynamic_colors", prefs.useDynamicColors)
            put("group_similar_stories", prefs.groupSimilarStories)
            put("bookmark_list_view_mode_is_grid", prefs.bookmarkListViewModeIsGrid)
            put("last_selected_bookmark_list_id", prefs.lastSelectedBookmarkListId)
            put("has_seen_swipe_hint", prefs.hasSeenSwipeHint)
            put("recent_searches", JSONArray(prefs.recentSearches))
        }
        backupObj.put("preferences", prefsObj)

        val feedsArr = JSONArray()
        for (feed in customFeeds) {
            val feedObj = JSONObject().apply {
                put("title", feed.title)
                put("url", feed.url)
                put("category", feed.category)
            }
            feedsArr.put(feedObj)
        }
        backupObj.put("custom_feeds", feedsArr)

        val articlesArr = JSONArray()
        // Link-only sources (headline + link publishers) are never exported: their data stays in the
        // device cache only and must not be restorable after the publisher withdraws the source.
        for (article in articles.filter { !LinkOnlySources.isLinkOnlyArticle(it) }) {
            val articleObj = JSONObject().apply {
                put("link", article.link)
                put("title", article.title)
                put("description", article.description)
                put("pubDate", article.pubDate)
                put("sourceName", article.sourceName)
                put("sourceUrl", article.sourceUrl)
                put("category", article.category)
                put("thumbnailUrl", article.thumbnailUrl ?: JSONObject.NULL)
                put("isBookmarked", article.isBookmarked)
                put("customFeedId", article.customFeedId ?: JSONObject.NULL)
                put("fullText", article.fullText ?: JSONObject.NULL)
                put("tags", article.tags ?: JSONObject.NULL)
                put("aiSummary", article.aiSummary ?: JSONObject.NULL)
                put("translatedTitle", article.translatedTitle ?: JSONObject.NULL)
                put("translatedBody", article.translatedBody ?: JSONObject.NULL)
                put("translatedLanguage", article.translatedLanguage ?: JSONObject.NULL)
                put("detectedLanguage", article.detectedLanguage ?: JSONObject.NULL)
                put("isRead", article.isRead)
            }
            articlesArr.put(articleObj)
        }
        backupObj.put("articles", articlesArr)

        val listsArr = JSONArray()
        for (list in bookmarkLists) {
            val listObj = JSONObject().apply {
                put("id", list.id)
                put("name", list.name)
                put("isDefault", list.isDefault)
                put("createdAt", list.createdAt)
            }
            listsArr.put(listObj)
        }
        backupObj.put("bookmark_lists", listsArr)

        val crossRefsArr = JSONArray()
        for (ref in crossRefs.filter { !LinkOnlySources.isLinkOnlyUrl(it.articleLink) }) {
            val refObj = JSONObject().apply {
                put("articleLink", ref.articleLink)
                put("listId", ref.listId)
            }
            crossRefsArr.put(refObj)
        }
        backupObj.put("article_bookmark_cross_refs", crossRefsArr)

        return backupObj.toString(4)
    }

    data class ParsedBackup(
        val customFeeds: List<CustomFeed>,
        val articles: List<Article>,
        val bookmarkLists: List<BookmarkList>,
        val crossRefs: List<ArticleBookmarkListCrossRef>,
        val applyPreferences: (PreferencesHelper) -> Unit
    )

    fun parseBackupJson(jsonString: String): ParsedBackup {
        val backupObj = JSONObject(jsonString)
        val version = backupObj.optInt("version", 1)
        if (version < 1) {
            throw IllegalArgumentException("Backup version $version is not supported.")
        }

        val customFeeds = mutableListOf<CustomFeed>()
        if (backupObj.has("custom_feeds")) {
            val feedsArr = backupObj.getJSONArray("custom_feeds")
            for (i in 0 until feedsArr.length()) {
                val feedObj = feedsArr.getJSONObject(i)
                val rawUrl = feedObj.optString("url", "")
                if (rawUrl.isEmpty()) continue
                val safeUrl = UrlSecurityValidator.sanitizeUrl(rawUrl, requireHttps = true)
                if (safeUrl == null) {
                    Log.w("StreamFolioBackup", "Skipping unsafe custom feed URL from backup: ${UrlSecurityValidator.normalizeUrl(rawUrl)?.toHttpUrlOrNull()?.host ?: "[invalid]"}")
                    continue
                }
                customFeeds.add(
                    CustomFeed(
                        title = feedObj.optString("title", "Custom Feed").take(200),
                        url = safeUrl,
                        category = feedObj.optString("category", "General").take(100)
                    )
                )
            }
        }

        val restoredArticles = mutableListOf<Article>()
        if (backupObj.has("articles")) {
            val articlesArr = backupObj.getJSONArray("articles")
            for (i in 0 until articlesArr.length()) {
                val articleObj = articlesArr.getJSONObject(i)
                val rawLink = articleObj.optString("link", "")
                if (rawLink.isEmpty()) continue
                val safeLink = UrlSecurityValidator.sanitizeUrl(rawLink, requireHttps = false)
                if (safeLink == null) {
                    Log.w("StreamFolioBackup", "Skipping unsafe article URL from backup: ${UrlSecurityValidator.normalizeUrl(rawLink)?.toHttpUrlOrNull()?.host ?: "[invalid]"}")
                    continue
                }
                if (LinkOnlySources.isLinkOnlyUrl(safeLink)) continue
                restoredArticles.add(
                    Article(
                        link = safeLink,
                        title = articleObj.optString("title", "Untitled").take(500),
                        description = articleObj.optString("description", "").take(2000),
                        pubDate = articleObj.optString("pubDate", "").take(100),
                        sourceName = articleObj.optString("sourceName", "Unknown").take(200),
                        sourceUrl = articleObj.optString("sourceUrl", "").take(500),
                        category = articleObj.optString("category", "General").take(100),
                        thumbnailUrl = if (articleObj.isNull("thumbnailUrl")) null else articleObj.optString("thumbnailUrl", "")?.take(500)?.let { UrlSecurityValidator.normalizeToHttps(it) },
                        isBookmarked = articleObj.optBoolean("isBookmarked", true),
                        customFeedId = if (articleObj.isNull("customFeedId")) null else articleObj.optInt("customFeedId"),
                        fullText = if (articleObj.isNull("fullText")) null else articleObj.optString("fullText", "")?.take(50_000),
                        tags = if (articleObj.isNull("tags")) null else articleObj.optString("tags", "")?.take(500),
                        aiSummary = if (articleObj.isNull("aiSummary")) null else articleObj.optString("aiSummary", "")?.take(10_000),
                        translatedTitle = if (articleObj.isNull("translatedTitle")) null else articleObj.optString("translatedTitle", "")?.take(500),
                        translatedBody = if (articleObj.isNull("translatedBody")) null else articleObj.optString("translatedBody", "")?.take(50_000),
                        translatedLanguage = if (articleObj.isNull("translatedLanguage")) null else articleObj.optString("translatedLanguage", "")?.take(50),
                        detectedLanguage = if (articleObj.isNull("detectedLanguage")) null else articleObj.optString("detectedLanguage", "")?.take(50),
                        isRead = articleObj.optBoolean("isRead", false)
                    )
                )
            }
        }

        val restoredLists = mutableListOf<BookmarkList>()
        if (backupObj.has("bookmark_lists")) {
            val listsArr = backupObj.getJSONArray("bookmark_lists")
            for (i in 0 until listsArr.length()) {
                val listObj = listsArr.getJSONObject(i)
                restoredLists.add(
                    BookmarkList(
                        id = listObj.getLong("id"),
                        name = listObj.optString("name", "List").take(100),
                        isDefault = listObj.optBoolean("isDefault", false),
                        createdAt = listObj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val restoredCrossRefs = mutableListOf<ArticleBookmarkListCrossRef>()
        if (backupObj.has("article_bookmark_cross_refs")) {
            val crossRefsArr = backupObj.getJSONArray("article_bookmark_cross_refs")
            for (i in 0 until crossRefsArr.length()) {
                val refObj = crossRefsArr.getJSONObject(i)
                val rawLink = refObj.getString("articleLink")
                val safeLink = UrlSecurityValidator.sanitizeUrl(rawLink, requireHttps = false)
                if (safeLink != null && !LinkOnlySources.isLinkOnlyUrl(safeLink)) {
                    restoredCrossRefs.add(
                        ArticleBookmarkListCrossRef(
                            articleLink = safeLink,
                            listId = refObj.getLong("listId")
                        )
                    )
                }
            }
        }

        val applyPrefs: (PreferencesHelper) -> Unit = { prefs ->
            if (backupObj.has("preferences")) {
                val prefsObj = backupObj.getJSONObject("preferences")
                if (prefsObj.has("language")) prefs.language = prefsObj.getString("language")
                if (prefsObj.has("region")) prefs.region = prefsObj.getString("region")
                
                if (prefsObj.has("selected_categories")) {
                    val catArr = prefsObj.getJSONArray("selected_categories")
                    val catSet = mutableSetOf<String>()
                    for (i in 0 until catArr.length()) {
                        catSet.add(catArr.getString(i))
                    }
                    prefs.selectedCategories = catSet
                }
                
                if (prefsObj.has("is_default_feeds_enabled")) {
                    prefs.isDefaultFeedsEnabled = prefsObj.getBoolean("is_default_feeds_enabled")
                }
                
                if (prefsObj.has("category_order")) {
                    val orderArr = prefsObj.getJSONArray("category_order")
                    val orderList = mutableListOf<String>()
                    for (i in 0 until orderArr.length()) {
                        orderList.add(orderArr.getString(i))
                    }
                    prefs.categoryOrder = orderList
                }

                if (prefsObj.has("disabled_feed_urls")) {
                    val disArr = prefsObj.getJSONArray("disabled_feed_urls")
                    val disSet = mutableSetOf<String>()
                    for (i in 0 until disArr.length()) {
                        disSet.add(disArr.getString(i))
                    }
                    prefs.disabledFeedUrls = disSet
                }

                if (prefsObj.has("enabled_cross_region_feeds")) {
                    val crossArr = prefsObj.getJSONArray("enabled_cross_region_feeds")
                    val crossSet = mutableSetOf<String>()
                    for (i in 0 until crossArr.length()) {
                        crossSet.add(crossArr.getString(i))
                    }
                    prefs.enabledCrossRegionFeeds = crossSet
                }

                if (prefsObj.has("cache_history_days")) {
                    val days = prefsObj.getInt("cache_history_days")
                    if (days in 1..36500) prefs.cacheHistoryDays = days
                }

                if (prefsObj.has("reader_font_family")) {
                    val font = prefsObj.getString("reader_font_family")
                    if (font in setOf("sans_serif", "serif")) prefs.readerFontFamily = font
                }

                if (prefsObj.has("reader_font_size")) {
                    val size = prefsObj.getDouble("reader_font_size").toFloat()
                    if (size in 12f..30f) prefs.readerFontSize = size
                }

                if (prefsObj.has("reader_line_spacing")) {
                    val spacing = prefsObj.getDouble("reader_line_spacing").toFloat()
                    if (spacing in 1.0f..3.0f) prefs.readerLineSpacing = spacing
                }

                if (prefsObj.has("is_ai_enabled")) {
                    prefs.isAiEnabled = prefsObj.getBoolean("is_ai_enabled")
                }

                if (prefsObj.has("is_translation_enabled")) {
                    prefs.isTranslationEnabled = prefsObj.getBoolean("is_translation_enabled")
                }

                if (prefsObj.has("is_summary_enabled")) {
                    prefs.isSummaryEnabled = prefsObj.getBoolean("is_summary_enabled")
                }

                if (prefsObj.has("is_smart_tags_enabled")) {
                    prefs.isSmartTagsEnabled = prefsObj.getBoolean("is_smart_tags_enabled")
                }

                if (prefsObj.has("has_seen_ai_spotlight")) {
                    prefs.hasSeenAiSpotlight = prefsObj.getBoolean("has_seen_ai_spotlight")
                }

                if (prefsObj.has("swipe_left_action")) {
                    val action = prefsObj.getString("swipe_left_action")
                    if (isValidSwipeAction(action)) prefs.swipeLeftAction = action
                }

                if (prefsObj.has("swipe_right_action")) {
                    val action = prefsObj.getString("swipe_right_action")
                    if (isValidSwipeAction(action)) prefs.swipeRightAction = action
                }

                if (prefsObj.has("tts_speech_rate")) {
                    val rate = prefsObj.getDouble("tts_speech_rate").toFloat()
                    if (rate in 0.5f..3.0f) prefs.ttsSpeechRate = rate
                }

                if (prefsObj.has("use_dynamic_colors")) {
                    prefs.useDynamicColors = prefsObj.getBoolean("use_dynamic_colors")
                }

                if (prefsObj.has("group_similar_stories")) {
                    prefs.groupSimilarStories = prefsObj.getBoolean("group_similar_stories")
                }

                if (prefsObj.has("bookmark_list_view_mode_is_grid")) {
                    prefs.bookmarkListViewModeIsGrid = prefsObj.getBoolean("bookmark_list_view_mode_is_grid")
                }

                if (prefsObj.has("last_selected_bookmark_list_id")) {
                    prefs.lastSelectedBookmarkListId = prefsObj.getLong("last_selected_bookmark_list_id")
                }

                if (prefsObj.has("has_seen_swipe_hint")) {
                    prefs.hasSeenSwipeHint = prefsObj.getBoolean("has_seen_swipe_hint")
                }

                if (prefsObj.has("recent_searches")) {
                    val searchesArr = prefsObj.getJSONArray("recent_searches")
                    val searchesList = mutableListOf<String>()
                    for (i in 0 until searchesArr.length()) {
                        searchesList.add(searchesArr.getString(i))
                    }
                    prefs.recentSearches = searchesList
                }
            }
        }

        return ParsedBackup(customFeeds, restoredArticles, restoredLists, restoredCrossRefs, applyPrefs)
    }

    private fun isValidSwipeAction(action: String?): Boolean {
        return action in setOf("bookmark", "share", "read", "none")
    }
}
