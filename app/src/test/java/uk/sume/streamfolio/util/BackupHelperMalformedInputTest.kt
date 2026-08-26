package uk.sume.streamfolio.util

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.json.JSONException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uk.sume.streamfolio.data.local.PreferencesHelper
import uk.sume.streamfolio.testutil.FakeContext
import uk.sume.streamfolio.testutil.FakeSharedPreferences

/**
 * P0 coverage for [BackupHelper.parseBackupJson] handling of malformed, truncated, or
 * malicious restore input (the user-supplied JSON restore path called out in
 * docs/01-architecture.md as having a length-clamping mitigation with no prior regression test).
 */
class BackupHelperMalformedInputTest {

    @Before
    fun stubAndroidLog() {
        // parseBackupJson() calls android.util.Log.w() when it skips an unsafe URL; the
        // Android unit-test stub jar throws "not mocked" for real android.* calls, so this is
        // stubbed with MockK rather than exercised against a real Log implementation.
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>()) } returns 0
    }

    @After
    fun unstubAndroidLog() {
        unmockkStatic(Log::class)
    }

    @Test(expected = JSONException::class)
    fun `blank input throws JSONException instead of returning empty backup`() {
        BackupHelper.parseBackupJson("")
    }

    @Test(expected = JSONException::class)
    fun `non-json garbage throws JSONException`() {
        BackupHelper.parseBackupJson("not json at all { [ }")
    }

    @Test(expected = JSONException::class)
    fun `truncated json throws JSONException`() {
        val truncated = """{"version": 2, "custom_feeds": [{"title": "Wired", "url":"""
        BackupHelper.parseBackupJson(truncated)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `version below 1 is rejected`() {
        BackupHelper.parseBackupJson("""{"version": 0}""")
    }

    @Test
    fun `missing optional sections do not crash and yield empty collections`() {
        val parsed = BackupHelper.parseBackupJson("""{"version": 2}""")
        assertTrue(parsed.customFeeds.isEmpty())
        assertTrue(parsed.articles.isEmpty())
        assertTrue(parsed.bookmarkLists.isEmpty())
        assertTrue(parsed.crossRefs.isEmpty())
    }

    @Test
    fun `unsafe custom feed and article urls are silently skipped, not restored`() {
        // language=JSON
        val json = """
            {
              "version": 2,
              "custom_feeds": [
                {"title": "Evil", "url": "http://169.254.169.254/latest/meta-data/", "category": "Tech"},
                {"title": "Fine", "url": "https://good.example.com/feed.xml", "category": "Tech"}
              ],
              "articles": [
                {"link": "http://127.0.0.1/admin", "title": "SSRF", "description": "", "pubDate": "", "sourceName": "", "sourceUrl": "", "category": "General", "isBookmarked": true, "isRead": false}
              ]
            }
        """.trimIndent()

        val parsed = BackupHelper.parseBackupJson(json)

        assertEquals(1, parsed.customFeeds.size)
        assertEquals("https://good.example.com/feed.xml", parsed.customFeeds[0].url)
        assertTrue("unsafe article link must be dropped, not restored", parsed.articles.isEmpty())
    }

    @Test
    fun `oversized string fields are clamped rather than stored unbounded`() {
        val hugeTitle = "A".repeat(10_000)
        val json = """
            {
              "version": 2,
              "articles": [
                {"link": "https://example.com/a", "title": "$hugeTitle", "description": "", "pubDate": "", "sourceName": "", "sourceUrl": "", "category": "General", "isBookmarked": true, "isRead": false}
              ]
            }
        """.trimIndent()

        val parsed = BackupHelper.parseBackupJson(json)

        assertEquals(1, parsed.articles.size)
        assertTrue(parsed.articles[0].title.length <= 500)
    }

    @Test
    fun `applyPreferences ignores an out-of-range preference instead of corrupting state`() {
        val json = """
            {
              "version": 2,
              "preferences": {
                "reader_font_size": 999.0,
                "cache_history_days": -5
              }
            }
        """.trimIndent()

        val fakePrefs = FakeSharedPreferences()
        val context = FakeContext(fakePrefs)
        val prefs = PreferencesHelper(context)
        val defaultFontSize = prefs.readerFontSize
        val defaultCacheDays = prefs.cacheHistoryDays

        val parsed = BackupHelper.parseBackupJson(json)
        parsed.applyPreferences(prefs)

        assertEquals("out-of-range font size must not be applied", defaultFontSize, prefs.readerFontSize)
        assertEquals("negative cache_history_days must not be applied", defaultCacheDays, prefs.cacheHistoryDays)
    }
}
