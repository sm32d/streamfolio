package uk.sume.news

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.sume.streamfolio.data.local.PreferencesHelper
import uk.sume.streamfolio.data.model.Article
import uk.sume.streamfolio.data.model.CustomFeed
import uk.sume.streamfolio.util.BackupHelper
import java.util.HashMap
import org.json.JSONObject

class BackupHelperTest {

    private class MockEditor : SharedPreferences.Editor {
        val map = HashMap<String, Any>()
        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            if (value != null) map[key] = value else map.remove(key)
            return this
        }
        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
            if (values != null) map[key] = values else map.remove(key)
            return this
        }
        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            map[key] = value
            return this
        }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            map[key] = value
            return this
        }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            map[key] = value
            return this
        }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            map[key] = value
            return this
        }
        override fun remove(key: String): SharedPreferences.Editor {
            map.remove(key)
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            map.clear()
            return this
        }
        override fun commit(): Boolean = true
        override fun apply() {}
    }

    private class MockSharedPreferences : SharedPreferences {
        val map = HashMap<String, Any>()
        val editor = MockEditor()

        override fun getAll(): Map<String, *> = map
        override fun getString(key: String, defValue: String?): String? = (map[key] as? String) ?: defValue
        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = (map[key] as? Set<String>) ?: defValues
        override fun getInt(key: String, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = editor.apply {
            editor.map.clear()
            editor.map.putAll(this@MockSharedPreferences.map)
        }
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        fun save() {
            map.clear()
            map.putAll(editor.map)
        }
    }

    private class MockContext(val sharedPreferences: SharedPreferences) : android.content.ContextWrapper(null) {
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
            return sharedPreferences
        }
        override fun getApplicationContext(): Context {
            return this
        }
    }

    @Test
    fun testSerializationDeserialization() {
        val mockPrefs = MockSharedPreferences()
        val context = MockContext(mockPrefs)

        val prefs = PreferencesHelper(context)
        
        // 1. Set initial preferences to non-defaults
        prefs.isAiEnabled = true
        prefs.swipeLeftAction = "read"
        prefs.swipeRightAction = "none"
        mockPrefs.save() // Save editor transactions to mock storage

        // 2. Generate backup JSON
        val customFeeds = listOf(CustomFeed(title = "Wired", url = "https://wired.com/feed", category = "Tech"))
        val articles = emptyList<Article>()
        val backupJson = BackupHelper.generateBackupJson(context, customFeeds, articles)

        println("Generated JSON:\n$backupJson")

        // Assert JSON structure
        val jsonObj = JSONObject(backupJson)
        assertTrue(jsonObj.has("preferences"))
        assertTrue(jsonObj.has("custom_feeds"))
        val prefsObj = jsonObj.getJSONObject("preferences")
        assertEquals(true, prefsObj.getBoolean("is_ai_enabled"))
        assertEquals("read", prefsObj.getString("swipe_left_action"))
        assertEquals("none", prefsObj.getString("swipe_right_action"))

        // 3. Clear preferences
        prefs.isAiEnabled = false
        prefs.swipeLeftAction = "bookmark"
        prefs.swipeRightAction = "share"
        mockPrefs.save()

        // 4. Parse & apply backup JSON
        val parsed = BackupHelper.parseBackupJson(backupJson)
        parsed.applyPreferences(prefs)
        mockPrefs.save()

        // 5. Verify restored values
        assertEquals(true, prefs.isAiEnabled)
        assertEquals("read", prefs.swipeLeftAction)
        assertEquals("none", prefs.swipeRightAction)
    }
}
