package uk.sume.streamfolio.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import uk.sume.streamfolio.testutil.AndroidTestSupport
import uk.sume.streamfolio.testutil.BaseAndroidUiTest

/**
 * On-device Compose UI tests for HomeScreen (P2).
 *
 * Fixture articles are registered via seedCustomFeedArticle() (a real CustomFeed + customFeedId),
 * not a bare Article insert: HomeScreen's real-category feed filter only shows an article if its
 * customFeedId is set or its sourceUrl matches a real curated feed for the device's region - a
 * fixture sourceUrl otherwise gets silently filtered out (see AndroidTestSupport's kdoc).
 *
 * A single seeded article renders via the "Trending" card path (TrendingCard), not the list path
 * (ArticleListItem) - HomeScreen shows the first 3 articles per category as trending cards and
 * only the rest in the scrollable list. Both composables carry the same
 * "article_item_<link>" testTag so tests don't need to care which path a given fixture count
 * lands in.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest : BaseAndroidUiTest() {

    @Test
    fun homeScreenShowsASeededArticleInTheDefaultCategory() {
        AndroidTestSupport.seedCustomFeedArticle(
            link = "https://example.com/home-test/top-stories-1",
            title = "Seeded Top Stories Headline For UI Test",
            category = "Top Stories"
        )

        launchApp().use {
            composeRule.waitUntilAtLeastOneExists(hasText("Seeded Top Stories Headline For UI Test"), timeoutMillis = 20_000)
            composeRule.onNodeWithText("Seeded Top Stories Headline For UI Test").assertIsDisplayed()
        }
    }

    @Test
    fun switchingCategoryTabShowsThatCategorysSeededArticle() {
        AndroidTestSupport.seedCustomFeedArticle(
            link = "https://example.com/home-test/top-stories-2",
            title = "Top Stories Fixture Article",
            category = "Top Stories"
        )
        AndroidTestSupport.seedCustomFeedArticle(
            link = "https://example.com/home-test/technology-1",
            title = "Technology Fixture Article",
            category = "Technology"
        )

        launchApp().use {
            composeRule.waitUntilAtLeastOneExists(hasText("Top Stories Fixture Article"), timeoutMillis = 20_000)

            composeRule.onNodeWithText("Technology").performClick()

            composeRule.waitUntilAtLeastOneExists(hasText("Technology Fixture Article"), timeoutMillis = 20_000)
            composeRule.onNodeWithText("Technology Fixture Article").assertIsDisplayed()
        }
    }

    @Test
    fun tappingAnArticleCardNavigatesToDetailScreen() {
        val link = "https://example.com/home-test/detail-nav"
        AndroidTestSupport.seedCustomFeedArticle(link = link, title = "Article That Opens Detail Screen", category = "Top Stories")

        launchApp().use {
            composeRule.waitUntilAtLeastOneExists(hasText("Article That Opens Detail Screen"), timeoutMillis = 20_000)

            composeRule.onNodeWithTag("article_item_$link").performClick()

            // DetailScreen's toolbar always has a "Back" IconButton, regardless of reader/web mode.
            composeRule.waitUntilAtLeastOneExists(hasContentDescription("Back"), timeoutMillis = 10_000)
            composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        }
    }

    @Test
    fun pullToRefreshGestureDoesNotCrashTheScreen() {
        AndroidTestSupport.seedCustomFeedArticle(
            link = "https://example.com/home-test/refresh",
            title = "Article Present Before Refresh",
            category = "Top Stories"
        )

        launchApp().use {
            composeRule.waitUntilAtLeastOneExists(hasText("Article Present Before Refresh"), timeoutMillis = 20_000)

            composeRule.onNodeWithTag("home_pull_to_refresh").performTouchInput { swipeDown() }

            // Regression guard, not a network assertion: the screen must still be showing the
            // article feed (not crashed/blank) after the gesture, whatever the refresh outcome.
            composeRule.onNodeWithTag("home_article_list").assertIsDisplayed()
        }
    }
}
