package uk.sume.streamfolio.ui.screens.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import uk.sume.streamfolio.testutil.AndroidTestSupport
import uk.sume.streamfolio.testutil.BaseAndroidUiTest

/**
 * On-device Compose UI tests covering the Settings hub and its 7 sub-screens (P2). One or two
 * representative tests per screen rather than exhaustive coverage of every control - see
 * docs/09-testing-and-quality.md for the full P2 scope note.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreensTest : BaseAndroidUiTest() {

    private fun navigateToSettingsHub() {
        composeRule.waitUntilAtLeastOneExists(hasText("Settings"), timeoutMillis = 20_000)
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Customise your StreamFolio experience."), timeoutMillis = 10_000)
    }

    @Test
    fun settingsHubShowsAllMenuSections() {
        launchApp()
        navigateToSettingsHub()

        composeRule.onNodeWithText("Preferences").assertIsDisplayed()
        composeRule.onNodeWithText("Swipe Gestures").assertIsDisplayed()
        composeRule.onNodeWithText("Manage Content & Sources").assertIsDisplayed()
        composeRule.onNodeWithText("Custom RSS Feeds").assertIsDisplayed()
        composeRule.onNodeWithText("Backup & Restore").assertIsDisplayed()
        composeRule.onNodeWithText("AI Services & Preferences").assertIsDisplayed()
    }

    @Test
    fun togglingGroupSimilarStoriesSwitchUpdatesPreference() {
        launchApp()
        navigateToSettingsHub()
        composeRule.onNodeWithText("Preferences").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Group Similar Stories"), timeoutMillis = 10_000)

        val defaultValue = AndroidTestSupport.prefs().groupSimilarStories
        composeRule.onNodeWithTag("group_similar_stories_switch").apply {
            if (defaultValue) assertIsOn() else assertIsOff()
        }

        composeRule.onNodeWithTag("group_similar_stories_switch").performClick()

        assertEquals(!defaultValue, AndroidTestSupport.prefs().groupSimilarStories)
    }

    @Test
    fun togglingSwipeGesturesScreenShowsItsHeader() {
        launchApp()
        navigateToSettingsHub()
        composeRule.onNodeWithText("Swipe Gestures").performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("Swipe Gesture Engine"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Swipe Gesture Engine").assertIsDisplayed()
    }

    @Test
    fun togglingAiMasterSwitchUpdatesPreference() {
        launchApp()
        navigateToSettingsHub()
        composeRule.onNodeWithText("AI Services & Preferences").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("ai_master_switch"), timeoutMillis = 10_000)

        // prefs.isAiEnabled defaults to false via AndroidTestSupport.resetAppState().
        assertEquals(false, AndroidTestSupport.prefs().isAiEnabled)
        composeRule.onNodeWithTag("ai_master_switch").assertIsOff()

        composeRule.onNodeWithTag("ai_master_switch").performClick()

        assertEquals(true, AndroidTestSupport.prefs().isAiEnabled)
    }

    @Test
    fun manageContentScreenNavigatesToReorderAndProviders() {
        launchApp()
        navigateToSettingsHub()
        composeRule.onNodeWithText("Manage Content & Sources").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Reorder Category Tabs"), timeoutMillis = 10_000)

        composeRule.onNodeWithText("Reorder Category Tabs").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Category Tab Layout Engine"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Category Tab Layout Engine").assertIsDisplayed()
    }

    @Test
    fun movingFirstCategoryDownReordersCategoryOrderPreference() {
        // With isDefaultFeedsEnabled = false (resetAppState()'s P2 baseline), the reorder list's
        // categories come entirely from customFeeds.map { it.category } - with none registered
        // it would be empty and render no rows at all (no "Move Down" button to click).
        AndroidTestSupport.seedCustomFeed(category = "Alpha Category")
        AndroidTestSupport.seedCustomFeed(category = "Beta Category")

        launchApp()
        navigateToSettingsHub()
        composeRule.onNodeWithText("Manage Content & Sources").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Reorder Category Tabs"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Reorder Category Tabs").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Alpha Category"), timeoutMillis = 10_000)

        // CustomFeedDao.getAllFeeds() orders by id DESC, so the screen's actual initial order is
        // [Beta Category, Alpha Category] (Beta seeded second = higher id, sorts first) - clicking
        // index 0's "Move Down" swaps that pair, yielding [Alpha Category, Beta Category].
        composeRule.onAllNodesWithContentDescription("Move Down")[0].performClick()
        val after = AndroidTestSupport.prefs().categoryOrder

        assertEquals(listOf("Alpha Category", "Beta Category"), after)
    }

    @Test
    fun defaultFeedProvidersScreenShowsItsHeader() {
        launchApp()
        navigateToSettingsHub()
        composeRule.onNodeWithText("Manage Content & Sources").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Default Feed Providers"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Default Feed Providers").performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("Default Feed Publishers"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Default Feed Publishers").assertIsDisplayed()
    }

    @Test
    fun addingACustomFeedShowsItInTheFeedsList() {
        launchApp()
        navigateToSettingsHub()
        composeRule.onNodeWithText("Custom RSS Feeds").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Custom RSS Curation Hub"), timeoutMillis = 10_000)

        composeRule.onNodeWithContentDescription("Add Feed").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Add New Feed"), timeoutMillis = 10_000)

        composeRule.onNodeWithTag("add_feed_title_field").performTextInput("My Test Feed")
        composeRule.onNodeWithTag("add_feed_url_field").performTextInput("https://example.com/my-feed.xml")
        composeRule.onNodeWithTag("add_feed_category_field").performTextInput("Testing")
        composeRule.onNodeWithTag("confirm_add_feed_button").performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("My Test Feed"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("My Test Feed").assertIsDisplayed()
    }

    @Test
    fun backupAndRestoreScreenShowsItsHeader() {
        launchApp()
        navigateToSettingsHub()
        composeRule.onNodeWithText("Backup & Restore").performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("Backup & Restore Hub"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Backup & Restore Hub").assertIsDisplayed()
    }
}
