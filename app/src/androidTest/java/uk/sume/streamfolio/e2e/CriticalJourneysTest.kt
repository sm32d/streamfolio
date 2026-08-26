package uk.sume.streamfolio.e2e

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import uk.sume.streamfolio.testutil.AndroidTestSupport
import uk.sume.streamfolio.testutil.BaseAndroidUiTest

/**
 * P3: a handful of critical, multi-screen user journeys, on top of the single-screen coverage
 * already in the ui.screens test classes (P2). These exist to catch a break in the seam between
 * screens/state (e.g. a navigation crash, a write on one screen not reflected on another) that
 * per-screen tests wouldn't - each one deliberately spans at least two screens/navigation hops.
 */
@RunWith(AndroidJUnit4::class)
class CriticalJourneysTest : BaseAndroidUiTest() {

    /**
     * Repeatedly clicks OnboardingScreen's "Continue" button (present on every step until the
     * last) then its final "Start Reading" button - deliberately not hardcoding a step count,
     * since OnboardingScreen's total step count itself varies with in-wizard choices
     * (showPreferences = isDefaultFeedsEnabled || (isAiEnabled && isTranslationEnabled)).
     */
    private fun ComposeTestRule.clickThroughOnboarding() {
        repeat(6) {
            if (onAllNodesWithText("Continue").fetchSemanticsNodes().isNotEmpty()) {
                onNodeWithText("Continue").performClick()
                waitForIdle()
            }
        }
        waitUntilAtLeastOneExists(hasText("Start Reading"), timeoutMillis = 10_000)
        onNodeWithText("Start Reading").performClick()
    }

    @Test
    fun onboardingWizardCompletesAndReachesHomeScreen() {
        // Override resetAppState()'s P2 baseline (which always marks onboarding complete so
        // screen tests land straight on Home) - this journey specifically needs the wizard itself.
        AndroidTestSupport.prefs().isCompletedOnboarding = false

        launchApp()
        composeRule.waitUntilAtLeastOneExists(hasText("Welcome to\nStreamFolio"), timeoutMillis = 20_000)

        composeRule.clickThroughOnboarding()

        // OnboardingScreen navigates to "home_screen" with popUpTo(inclusive) on completion -
        // landing on Home (not stuck on/back to onboarding) is the behavior under test.
        composeRule.waitUntilAtLeastOneExists(hasContentDescription("Home"), timeoutMillis = 20_000)
        composeRule.onNodeWithContentDescription("Home").assertIsDisplayed()
        assertTrue("onboarding completion should persist", AndroidTestSupport.prefs().isCompletedOnboarding)
    }

    @Test
    fun pullToRefreshThenOpeningAnArticleDoesNotCrashTheSharedElementTransition() {
        val link = "https://example.com/e2e-test/refresh-then-open"
        AndroidTestSupport.seedCustomFeedArticle(link = link, title = "Refresh Then Open Journey Article", category = "Top Stories")

        launchApp()
        composeRule.waitUntilAtLeastOneExists(hasText("Refresh Then Open Journey Article"), timeoutMillis = 20_000)

        composeRule.onNodeWithTag("home_pull_to_refresh").performTouchInput { swipeDown() }
        composeRule.waitUntilAtLeastOneExists(hasText("Refresh Then Open Journey Article"), timeoutMillis = 10_000)

        // The shared-element transition between HomeScreen's card and DetailScreen's header is
        // exactly what a stale/cancelled refresh coroutine could leave in a bad state - reaching
        // a stable DetailScreen (not a crash/blank screen) after a refresh is the assertion here.
        composeRule.onNodeWithTag("article_item_$link").performClick()
        composeRule.waitUntilAtLeastOneExists(hasContentDescription("Back"), timeoutMillis = 10_000)
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun bookmarkingFromDetailScreenMakesItAppearUnderSavedCollections() {
        val link = "https://example.com/e2e-test/bookmark-then-saved"
        AndroidTestSupport.seedCustomFeedArticle(link = link, title = "Bookmark Then Saved Journey Article", category = "Top Stories")

        launchApp()
        composeRule.waitUntilAtLeastOneExists(hasText("Bookmark Then Saved Journey Article"), timeoutMillis = 20_000)
        composeRule.onNodeWithTag("article_item_$link").performClick()
        composeRule.waitUntilAtLeastOneExists(hasContentDescription("Save"), timeoutMillis = 10_000)

        composeRule.onNodeWithContentDescription("Save").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Save to Collection"), timeoutMillis = 10_000)
        // See SharedComponentsTest for why this wait is required before "Done" is safe to click.
        composeRule.waitUntilAtLeastOneExists(androidx.compose.ui.test.isOn(), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Done").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Saved to", substring = true), timeoutMillis = 10_000)

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Saved"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Saved").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("All Saved"), timeoutMillis = 10_000)

        composeRule.onNodeWithText("All Saved").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Bookmark Then Saved Journey Article"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Bookmark Then Saved Journey Article").assertIsDisplayed()

        val bookmarked = runBlocking { AndroidTestSupport.db().articleDao().getArticleByLink(link) }
        assertTrue("expected the article to be bookmarked", bookmarked?.isBookmarked == true)
    }

    @Test
    fun searchingForAnArticleAndOpeningItReachesDetailScreen() {
        AndroidTestSupport.seedArticle(
            AndroidTestSupport.article(
                link = "https://example.com/e2e-test/search-then-open",
                title = "Uniquely Searchable Journey Headline Wibble"
            )
        )

        launchApp()
        composeRule.waitUntilAtLeastOneExists(hasText("Search"), timeoutMillis = 20_000)
        composeRule.onNodeWithText("Search").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Search articles and topics"), timeoutMillis = 10_000)

        composeRule.onNodeWithTag("search_text_field").performTextInput("Wibble")
        composeRule.waitUntilAtLeastOneExists(hasText("Uniquely Searchable Journey Headline Wibble"), timeoutMillis = 10_000)

        composeRule.onNodeWithText("Uniquely Searchable Journey Headline Wibble").performClick()

        composeRule.waitUntilAtLeastOneExists(hasContentDescription("Back"), timeoutMillis = 10_000)
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun withAiDisabledDetailScreenNeverShowsTheAiKeyInsightsCard() {
        // resetAppState() already sets isAiEnabled = false (the P2 baseline) - asserting its
        // absence here, explicitly, is what turns that baseline fact into a regression guard:
        // a future change that starts ignoring the AI master switch would fail this test.
        assertFalse(AndroidTestSupport.prefs().isAiEnabled)

        val link = "https://example.com/e2e-test/ai-disabled"
        AndroidTestSupport.seedCustomFeedArticle(link = link, title = "AI Disabled Journey Article", category = "Top Stories")

        launchApp()
        composeRule.waitUntilAtLeastOneExists(hasText("AI Disabled Journey Article"), timeoutMillis = 20_000)
        composeRule.onNodeWithTag("article_item_$link").performClick()
        composeRule.waitUntilAtLeastOneExists(hasContentDescription("Back"), timeoutMillis = 10_000)

        // Give the screen a real window to have shown the card if the master switch were (bug)
        // ignored, rather than asserting absence in the same frame as the screen appearing.
        Thread.sleep(2_000)
        composeRule.onAllNodesWithText("AI Key Insights").assertCountEquals(0)
    }
}
