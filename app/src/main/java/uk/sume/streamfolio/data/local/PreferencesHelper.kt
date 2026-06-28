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
}
