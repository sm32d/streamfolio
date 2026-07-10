package uk.sume.streamfolio.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("news_prefs", Context.MODE_PRIVATE)

    var isCompletedOnboarding: Boolean
        get() = prefs.getBoolean("completed_onboarding", false)
        set(value) = prefs.edit().putBoolean("completed_onboarding", value).apply()

    var language: String
        get() = prefs.getString("language", "en") ?: "en"
        set(value) = prefs.edit().putString("language", value).apply()

    var region: String
        get() = prefs.getString("region", "US") ?: "US"
        set(value) = prefs.edit().putString("region", value).apply()

    var selectedCategories: Set<String>
        get() = prefs.getStringSet("selected_categories", setOf("Top Stories", "World", "Business", "Technology", "Science", "Sports", "Health", "Entertainment")) ?: setOf("Top Stories", "World", "Business", "Technology", "Science", "Sports", "Health", "Entertainment")
        set(value) = prefs.edit().putStringSet("selected_categories", value).apply()

    var isDefaultFeedsEnabled: Boolean
        get() = prefs.getBoolean("google_news_enabled", true)
        set(value) = prefs.edit().putBoolean("google_news_enabled", value).apply()

    var categoryOrder: List<String>
        get() {
            val orderStr = prefs.getString("category_order", "Top Stories,World,Business,Technology,Science,Sports,Health,Entertainment") ?: "Top Stories,World,Business,Technology,Science,Sports,Health,Entertainment"
            return orderStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
        set(value) = prefs.edit().putString("category_order", value.joinToString(",")).apply()

    var disabledFeedUrls: Set<String>
        get() = prefs.getStringSet("disabled_feed_urls", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("disabled_feed_urls", value).apply()

    var enabledCrossRegionFeeds: Set<String>
        get() = prefs.getStringSet("enabled_cross_region_feeds", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("enabled_cross_region_feeds", value).apply()

    var cacheHistoryDays: Int
        get() = prefs.getInt("cache_history_days", 36500)
        set(value) = prefs.edit().putInt("cache_history_days", value).apply()

    var readerFontFamily: String
        get() = prefs.getString("reader_font_family", "sans_serif") ?: "sans_serif"
        set(value) = prefs.edit().putString("reader_font_family", value).apply()

    var readerFontSize: Float
        get() = prefs.getFloat("reader_font_size", 16f)
        set(value) = prefs.edit().putFloat("reader_font_size", value).apply()

    var readerLineSpacing: Float
        get() = prefs.getFloat("reader_line_spacing", 1.5f)
        set(value) = prefs.edit().putFloat("reader_line_spacing", value).apply()

    var isAiEnabled: Boolean
        get() = prefs.getBoolean("ai_enabled", false)
        set(value) = prefs.edit().putBoolean("ai_enabled", value).apply()

    var isTranslationEnabled: Boolean
        get() = prefs.getBoolean("ai_translation_enabled", true)
        set(value) = prefs.edit().putBoolean("ai_translation_enabled", value).apply()

    var isSummaryEnabled: Boolean
        get() = prefs.getBoolean("ai_summary_enabled", true)
        set(value) = prefs.edit().putBoolean("ai_summary_enabled", value).apply()

    var isSmartTagsEnabled: Boolean
        get() = prefs.getBoolean("ai_smart_tags_enabled", true)
        set(value) = prefs.edit().putBoolean("ai_smart_tags_enabled", value).apply()

    var translationTargetLanguage: String
        get() = language
        set(value) { language = value }

    var hasSeenAiSpotlight: Boolean
        get() = prefs.getBoolean("has_seen_ai_spotlight", false)
        set(value) = prefs.edit().putBoolean("has_seen_ai_spotlight", value).apply()

    var hasClearedOldTags: Boolean
        get() = prefs.getBoolean("has_cleared_old_tags_v3", false)
        set(value) = prefs.edit().putBoolean("has_cleared_old_tags_v3", value).apply()

    var swipeLeftAction: String
        get() = prefs.getString("swipe_left_action", "bookmark") ?: "bookmark"
        set(value) = prefs.edit().putString("swipe_left_action", value).apply()

    var swipeRightAction: String
        get() = prefs.getString("swipe_right_action", "share") ?: "share"
        set(value) = prefs.edit().putString("swipe_right_action", value).apply()
}
