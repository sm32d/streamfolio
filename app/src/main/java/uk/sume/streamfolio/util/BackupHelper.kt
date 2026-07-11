package uk.sume.streamfolio.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import uk.sume.streamfolio.data.local.PreferencesHelper
import uk.sume.streamfolio.data.model.CustomFeed

object BackupHelper {

    fun generateBackupJson(context: Context, customFeeds: List<CustomFeed>): String {
        val prefs = PreferencesHelper(context)
        val backupObj = JSONObject()
        backupObj.put("version", 1)

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

        return backupObj.toString(4)
    }

    data class ParsedBackup(
        val customFeeds: List<CustomFeed>,
        val applyPreferences: (PreferencesHelper) -> Unit
    )

    fun parseBackupJson(jsonString: String): ParsedBackup {
        val backupObj = JSONObject(jsonString)
        val customFeeds = mutableListOf<CustomFeed>()
        if (backupObj.has("custom_feeds")) {
            val feedsArr = backupObj.getJSONArray("custom_feeds")
            for (i in 0 until feedsArr.length()) {
                val feedObj = feedsArr.getJSONObject(i)
                customFeeds.add(
                    CustomFeed(
                        title = feedObj.getString("title"),
                        url = feedObj.getString("url"),
                        category = feedObj.getString("category")
                    )
                )
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
                    prefs.cacheHistoryDays = prefsObj.getInt("cache_history_days")
                }
                
                if (prefsObj.has("reader_font_family")) {
                    prefs.readerFontFamily = prefsObj.getString("reader_font_family")
                }
                
                if (prefsObj.has("reader_font_size")) {
                    prefs.readerFontSize = prefsObj.getDouble("reader_font_size").toFloat()
                }
                
                if (prefsObj.has("reader_line_spacing")) {
                    prefs.readerLineSpacing = prefsObj.getDouble("reader_line_spacing").toFloat()
                }

                if (prefsObj.has("is_ai_enabled")) {
                    prefs.isAiEnabled = prefsObj.getBoolean("is_ai_enabled")
                    android.util.Log.d("StreamFolioBackup", "AI Enabled Restored: ${prefs.isAiEnabled}")
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
                    prefs.swipeLeftAction = prefsObj.getString("swipe_left_action")
                    android.util.Log.d("StreamFolioBackup", "Swipe Left Restored: ${prefs.swipeLeftAction}")
                }

                if (prefsObj.has("swipe_right_action")) {
                    prefs.swipeRightAction = prefsObj.getString("swipe_right_action")
                    android.util.Log.d("StreamFolioBackup", "Swipe Right Restored: ${prefs.swipeRightAction}")
                }
            }
        }

        return ParsedBackup(customFeeds, applyPrefs)
    }
}
