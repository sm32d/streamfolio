package uk.sume.streamfolio.data.network

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Where the remote source list lives: a static JSON file in the project repository (no server).
 * Format: `{"version":1,"sources":{"<source id>":{"enabled":true}}}`. A source that is missing
 * from the file, or has `enabled` false, is treated as withdrawn.
 */
object SourceConfig {
    const val URL = "https://raw.githubusercontent.com/sm32d/streamfolio/master/source-config/sources.json"

    /** How often a device re-checks the file (a launch/refresh inside this window reuses the last result). */
    const val CHECK_INTERVAL_MS = 60L * 60L * 1000L

    /**
     * Returns the ids of enabled sources, or null when [json] is not a valid source list
     * (in which case the caller keeps its last known state).
     */
    fun parseEnabledIds(json: String): Set<String>? {
        return try {
            val root = JSONObject(json)
            val sources = root.optJSONObject("sources") ?: return null
            val enabled = mutableSetOf<String>()
            val keys = sources.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                if (sources.optJSONObject(id)?.optBoolean("enabled", false) == true) {
                    enabled.add(id)
                }
            }
            enabled
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * On-device state for link-only sources: the last known remote enabled list and per-feed
 * throttle / conditional-request data. Kept in its own preferences file.
 */
class LinkOnlyStateStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("link_only_state", Context.MODE_PRIVATE)

    /** Ids of sources the remote list currently enables. Empty until a list has been fetched. */
    var enabledIds: Set<String>
        get() = prefs.getStringSet(KEY_ENABLED, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_ENABLED, HashSet(value)).apply()

    var configCheckedAt: Long
        get() = prefs.getLong(KEY_CHECKED_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_CHECKED_AT, value).apply()

    fun isEnabled(sourceId: String): Boolean = enabledIds.contains(sourceId)

    /** Whether the feed or article URL may be fetched at all. Non link-only URLs always may. */
    fun isUrlAllowed(url: String?): Boolean {
        val source = LinkOnlySources.forUrl(url) ?: return true
        return isEnabled(source.id)
    }

    fun nextFetchAt(feedUrl: String): Long = prefs.getLong(KEY_NEXT + feedUrl, 0L)
    fun setNextFetchAt(feedUrl: String, value: Long) = prefs.edit().putLong(KEY_NEXT + feedUrl, value).apply()

    fun etag(feedUrl: String): String? = prefs.getString(KEY_ETAG + feedUrl, null)
    fun lastModified(feedUrl: String): String? = prefs.getString(KEY_LAST_MODIFIED + feedUrl, null)

    fun saveValidators(feedUrl: String, etag: String?, lastModified: String?) {
        prefs.edit()
            .putString(KEY_ETAG + feedUrl, etag)
            .putString(KEY_LAST_MODIFIED + feedUrl, lastModified)
            .apply()
    }

    /** Drops all throttle and validator state (used when a source is withdrawn). */
    fun clearFeedState() {
        val editor = prefs.edit()
        prefs.all.keys
            .filter { it.startsWith(KEY_NEXT) || it.startsWith(KEY_ETAG) || it.startsWith(KEY_LAST_MODIFIED) }
            .forEach { editor.remove(it) }
        editor.apply()
    }

    private companion object {
        const val KEY_ENABLED = "enabled_ids"
        const val KEY_CHECKED_AT = "config_checked_at"
        const val KEY_NEXT = "next_fetch:"
        const val KEY_ETAG = "etag:"
        const val KEY_LAST_MODIFIED = "last_modified:"
    }
}
