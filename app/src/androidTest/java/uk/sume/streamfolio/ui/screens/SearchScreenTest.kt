package uk.sume.streamfolio.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import uk.sume.streamfolio.testutil.AndroidTestSupport
import uk.sume.streamfolio.testutil.BaseAndroidUiTest

/**
 * On-device Compose UI tests for SearchScreen (P2). Search results come from
 * NewsRepository.searchArticlesLocal() (a local Room LIKE query, no network), so fixtures use a
 * plain seedArticle() - no CustomFeed registration needed here, unlike HomeScreen's category
 * feed (see AndroidTestSupport's kdoc).
 */
@RunWith(AndroidJUnit4::class)
class SearchScreenTest : BaseAndroidUiTest() {

    private fun navigateToSearchTab() {
        composeRule.waitUntilAtLeastOneExists(hasText("Search"), timeoutMillis = 20_000)
        composeRule.onNodeWithText("Search").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Search articles and topics"), timeoutMillis = 10_000)
        // AppNavigation's tab enterTransition/exitTransition (tween(300)) keeps the outgoing
        // Home screen composed alongside the incoming Search screen for the animation's
        // duration; asserting on text before it settles intermittently found two live nodes.
        Thread.sleep(400)
        composeRule.waitForIdle()
    }

    @Test
    fun emptyQueryShowsFallbackTrendingTopics() {
        launchApp()
        navigateToSearchTab()

        // With no articles registered under any active feed, trendingTopics falls back to this
        // fixed list (NewsViewModel.trendingTopics) - deterministic without seeding anything.
        // "Markets" is used deliberately: several of the other fallback words ("AI", "Climate",
        // "Health", "Sports", "Space") are also substrings of the header's own infinitely-cycling
        // animated placeholder text (SearchScreen's searchSuggestions typewriter effect), which
        // intermittently collides with onNodeWithText's substring matching. "Markets"/"Tech"/
        // "Science" don't appear in that suggestions list.
        composeRule.waitUntilAtLeastOneExists(hasText("Trending Topics"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Markets").assertIsDisplayed()
    }

    @Test
    fun typingAQueryShowsAMatchingLocalArticle() {
        AndroidTestSupport.seedArticle(
            AndroidTestSupport.article(
                link = "https://example.com/search-test/match",
                title = "Unique Searchable Headline Xyzzy"
            )
        )

        launchApp()
        navigateToSearchTab()

        composeRule.onNodeWithTag("search_text_field").performTextInput("Xyzzy")

        composeRule.waitUntilAtLeastOneExists(hasText("Unique Searchable Headline Xyzzy"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Unique Searchable Headline Xyzzy").assertIsDisplayed()
    }

    @Test
    fun queryWithNoMatchesShowsNoResultsState() {
        launchApp()
        navigateToSearchTab()

        composeRule.onNodeWithTag("search_text_field").performTextInput("NoSuchArticleExistsAnywhereZzz")

        composeRule.waitUntilAtLeastOneExists(hasText("No articles found"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("No articles found").assertIsDisplayed()
    }

    @Test
    fun clickingATrendingTopicPopulatesSearchAndRecentSearches() {
        launchApp()
        navigateToSearchTab()

        composeRule.waitUntilAtLeastOneExists(hasText("Markets"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Markets").performClick()

        // Clearing the (now non-empty) query returns to the EMPTY state, which shows Recent
        // Searches once at least one exists - confirms the tap both set the query and recorded it.
        composeRule.onNodeWithContentDescription("Clear").performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("Recent Searches"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Recent Searches").assertIsDisplayed()
    }
}
