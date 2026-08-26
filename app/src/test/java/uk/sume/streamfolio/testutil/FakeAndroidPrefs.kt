package uk.sume.streamfolio.testutil

import android.content.Context
import android.content.SharedPreferences

/**
 * Minimal in-memory [SharedPreferences]/[Context] fakes for JVM unit tests that need a
 * [uk.sume.streamfolio.data.local.PreferencesHelper] without Robolectric. Mirrors the pattern
 * already used privately in `uk.sume.news.BackupHelperTest`; pulled out here so new tests under
 * `uk.sume.streamfolio` can reuse it instead of re-declaring the same boilerplate.
 */
class FakeSharedPreferences : SharedPreferences {
    private val map = HashMap<String, Any>()
    private val editor = FakeEditor(this)

    override fun getAll(): Map<String, *> = map
    override fun getString(key: String, defValue: String?): String? = (map[key] as? String) ?: defValue
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = (map[key] as? Set<String>) ?: defValues
    override fun getInt(key: String, defValue: Int): Int = (map[key] as? Int) ?: defValue
    override fun getLong(key: String, defValue: Long): Long = (map[key] as? Long) ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = (map[key] as? Float) ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
    override fun contains(key: String): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = editor
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    internal fun applyChanges(pending: Map<String, Any?>) {
        for ((key, value) in pending) {
            if (value == null) map.remove(key) else map[key] = value
        }
    }
}

class FakeEditor(private val target: FakeSharedPreferences) : SharedPreferences.Editor {
    private val pending = HashMap<String, Any?>()

    override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { pending[key] = value }
    override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor = apply { pending[key] = values }
    override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply { pending[key] = value }
    override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply { pending[key] = value }
    override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply { pending[key] = value }
    override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply { pending[key] = value }
    override fun remove(key: String): SharedPreferences.Editor = apply { pending[key] = null }
    override fun clear(): SharedPreferences.Editor = apply { pending.clear() }
    override fun commit(): Boolean {
        apply()
        return true
    }
    override fun apply() {
        target.applyChanges(pending)
        pending.clear()
    }
}

class FakeContext(private val prefs: FakeSharedPreferences) : android.content.ContextWrapper(null) {
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
    override fun getApplicationContext(): Context = this
}
