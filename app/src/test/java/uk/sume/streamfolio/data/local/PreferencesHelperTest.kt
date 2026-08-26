package uk.sume.streamfolio.data.local

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Round-trips every stored preference key through a real (Robolectric-backed)
 * SharedPreferences instance. compileSdk is 36 (Android 16 preview); pinned to sdk = 34 since
 * Robolectric's support for 36 has not been verified against this repo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PreferencesHelperTest {

    private lateinit var prefs: PreferencesHelper

    @Before
    fun setUp() {
        prefs = PreferencesHelper(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `defaults match documented fallback values before anything is written`() {
        assertEquals(false, prefs.isCompletedOnboarding)
        assertEquals("en", prefs.language)
        assertEquals("US", prefs.region)
        assertEquals(
            setOf("Top Stories", "World", "Business", "Technology", "Science", "Sports", "Health", "Entertainment"),
            prefs.selectedCategories
        )
        assertEquals(true, prefs.isDefaultFeedsEnabled)
        assertEquals(
            listOf("Top Stories", "World", "Business", "Technology", "Science", "Sports", "Health", "Entertainment"),
            prefs.categoryOrder
        )
        assertEquals(emptySet<String>(), prefs.disabledFeedUrls)
        assertEquals(emptySet<String>(), prefs.enabledCrossRegionFeeds)
        assertEquals(36500, prefs.cacheHistoryDays)
        assertEquals("sans_serif", prefs.readerFontFamily)
        assertEquals(16f, prefs.readerFontSize)
        assertEquals(1.5f, prefs.readerLineSpacing)
        assertEquals(false, prefs.isAiEnabled)
        assertEquals(true, prefs.isTranslationEnabled)
        assertEquals(true, prefs.isSummaryEnabled)
        assertEquals(true, prefs.isSmartTagsEnabled)
        assertEquals(false, prefs.hasSeenAiSpotlight)
        assertEquals(false, prefs.hasClearedOldTags)
        assertEquals("bookmark", prefs.swipeLeftAction)
        assertEquals("share", prefs.swipeRightAction)
        assertEquals(1.0f, prefs.ttsSpeechRate)
        assertEquals(false, prefs.useDynamicColors)
        assertEquals(emptyList<String>(), prefs.recentSearches)
        assertEquals(false, prefs.hasSeenSwipeHint)
        assertEquals(true, prefs.groupSimilarStories)
        assertEquals(true, prefs.bookmarkListViewModeIsGrid)
        assertEquals(-1L, prefs.lastSelectedBookmarkListId)
    }

    @Test
    fun `every preference round-trips through a fresh PreferencesHelper instance backed by the same store`() {
        prefs.isCompletedOnboarding = true
        prefs.language = "fr"
        prefs.region = "FR"
        prefs.selectedCategories = setOf("World", "Technology")
        prefs.isDefaultFeedsEnabled = false
        prefs.categoryOrder = listOf("Technology", "World")
        prefs.disabledFeedUrls = setOf("https://blocked.example.com/feed.xml")
        prefs.enabledCrossRegionFeeds = setOf("US|Tech|https://example.com/feed.xml")
        prefs.cacheHistoryDays = 30
        prefs.readerFontFamily = "serif"
        prefs.readerFontSize = 22f
        prefs.readerLineSpacing = 2.0f
        prefs.isAiEnabled = true
        prefs.isTranslationEnabled = false
        prefs.isSummaryEnabled = false
        prefs.isSmartTagsEnabled = false
        prefs.hasSeenAiSpotlight = true
        prefs.hasClearedOldTags = true
        prefs.swipeLeftAction = "share"
        prefs.swipeRightAction = "read"
        prefs.ttsSpeechRate = 1.5f
        prefs.useDynamicColors = true
        prefs.recentSearches = listOf("cats", "dogs")
        prefs.hasSeenSwipeHint = true
        prefs.groupSimilarStories = false
        prefs.bookmarkListViewModeIsGrid = false
        prefs.lastSelectedBookmarkListId = 42L

        // A second instance over the same underlying SharedPreferences file simulates reading
        // back after process death, not just re-reading the same live object's cached values.
        val reread = PreferencesHelper(ApplicationProvider.getApplicationContext())

        assertEquals(true, reread.isCompletedOnboarding)
        assertEquals("fr", reread.language)
        assertEquals("FR", reread.region)
        assertEquals(setOf("World", "Technology"), reread.selectedCategories)
        assertEquals(false, reread.isDefaultFeedsEnabled)
        assertEquals(listOf("Technology", "World"), reread.categoryOrder)
        assertEquals(setOf("https://blocked.example.com/feed.xml"), reread.disabledFeedUrls)
        assertEquals(setOf("US|Tech|https://example.com/feed.xml"), reread.enabledCrossRegionFeeds)
        assertEquals(30, reread.cacheHistoryDays)
        assertEquals("serif", reread.readerFontFamily)
        assertEquals(22f, reread.readerFontSize)
        assertEquals(2.0f, reread.readerLineSpacing)
        assertEquals(true, reread.isAiEnabled)
        assertEquals(false, reread.isTranslationEnabled)
        assertEquals(false, reread.isSummaryEnabled)
        assertEquals(false, reread.isSmartTagsEnabled)
        assertEquals(true, reread.hasSeenAiSpotlight)
        assertEquals(true, reread.hasClearedOldTags)
        assertEquals("share", reread.swipeLeftAction)
        assertEquals("read", reread.swipeRightAction)
        assertEquals(1.5f, reread.ttsSpeechRate)
        assertEquals(true, reread.useDynamicColors)
        assertEquals(listOf("cats", "dogs"), reread.recentSearches)
        assertEquals(true, reread.hasSeenSwipeHint)
        assertEquals(false, reread.groupSimilarStories)
        assertEquals(false, reread.bookmarkListViewModeIsGrid)
        assertEquals(42L, reread.lastSelectedBookmarkListId)
    }

    @Test
    fun `recentSearches is capped at 10 entries on write`() {
        prefs.recentSearches = (1..15).map { "query$it" }
        assertEquals(10, prefs.recentSearches.size)
        assertEquals((1..10).map { "query$it" }, prefs.recentSearches)
    }

    @Test
    fun `translationTargetLanguage is an alias for language`() {
        prefs.translationTargetLanguage = "ja"
        assertEquals("ja", prefs.language)
        prefs.language = "ko"
        assertEquals("ko", prefs.translationTargetLanguage)
    }

    @Test
    fun `categoryOrder splitting trims whitespace and drops empty segments`() {
        // categoryOrder is stored as a single comma-joined string; a value written with stray
        // whitespace/empty segments (e.g. hand-edited backup JSON) must not resurrect blank
        // category entries on read-back.
        prefs.categoryOrder = listOf("World", "", "  Technology  ", "Business")
        assertEquals(listOf("World", "Technology", "Business"), prefs.categoryOrder)
    }
}
