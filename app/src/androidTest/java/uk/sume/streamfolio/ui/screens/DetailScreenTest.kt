package uk.sume.streamfolio.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import uk.sume.streamfolio.testutil.AndroidTestSupport
import uk.sume.streamfolio.testutil.BaseAndroidUiTest

/**
 * On-device Compose UI tests for DetailScreen (P2). DetailScreen is only reachable with a
 * non-null NewsViewModel.currentArticleDetail, which is set by HomeScreen's onTap handler
 * (selectArticleForDetail) before navigating - tests get there the same way a real user does,
 * by seeding a fixture article and tapping into it from Home, rather than deep-linking directly.
 */
@RunWith(AndroidJUnit4::class)
class DetailScreenTest : BaseAndroidUiTest() {

    private fun openArticleFromHome(link: String, title: String) {
        AndroidTestSupport.seedCustomFeedArticle(link = link, title = title, category = "Top Stories")
        launchApp()
        composeRule.waitUntilAtLeastOneExists(hasText(title), timeoutMillis = 20_000)
        composeRule.onNodeWithTag("article_item_$link").performClick()
        composeRule.waitUntilAtLeastOneExists(hasContentDescription("Back"), timeoutMillis = 10_000)
    }

    @Test
    fun readerAndWebViewTabsToggleWithoutCrashing() {
        openArticleFromHome("https://example.com/detail-test/toggle", "Reader Web Toggle Article")

        composeRule.onNodeWithText("Reader Web Toggle Article").assertIsDisplayed()

        composeRule.onNodeWithText("Web View").performClick()
        composeRule.waitUntilAtLeastOneExists(hasContentDescription("Back"), timeoutMillis = 10_000)

        composeRule.onNodeWithText("Reader").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Reader Web Toggle Article"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Reader Web Toggle Article").assertIsDisplayed()
    }

    @Test
    fun backButtonReturnsToHomeScreen() {
        val link = "https://example.com/detail-test/back-nav"
        openArticleFromHome(link, "Article For Back Navigation Test")

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.waitUntilAtLeastOneExists(hasTestTag("home_article_list"), timeoutMillis = 10_000)
        composeRule.onNodeWithTag("home_article_list").assertIsDisplayed()
    }

    @Test
    fun alsoCoveredByPanelExpandsAndNavigatesToSimilarArticle() {
        val leaderLink = "https://example.com/detail-test/leader"
        val followerLink = "https://example.com/detail-test/follower"

        // assignGroupIds' "group of one" convention: a leader's own groupId is its own link.
        // loadSimilarArticles() does an indexed WHERE groupId = ... lookup keyed on that value.
        AndroidTestSupport.seedCustomFeedArticle(
            link = leaderLink,
            title = "Leader Story With Coverage",
            category = "Top Stories",
            groupId = leaderLink
        )
        AndroidTestSupport.seedArticle(
            AndroidTestSupport.article(
                link = followerLink,
                title = "Follower Coverage From Another Source",
                sourceName = "Other Outlet",
                groupId = leaderLink
            )
        )

        launchApp()
        composeRule.waitUntilAtLeastOneExists(hasText("Leader Story With Coverage"), timeoutMillis = 20_000)
        composeRule.onNodeWithTag("article_item_$leaderLink").performClick()
        composeRule.waitUntilAtLeastOneExists(hasContentDescription("Back"), timeoutMillis = 10_000)

        // Panel row showing the follower's title/source, before it's expanded.
        composeRule.waitUntilAtLeastOneExists(hasText("Also covered by 1 other source"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Also covered by 1 other source").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Follower Coverage From Another Source"), timeoutMillis = 10_000)

        // Tapping the panel row navigates to the follower's own DetailScreen - its title
        // reappearing (as the new screen's content, not the now-gone panel row) confirms it.
        composeRule.onNodeWithText("Follower Coverage From Another Source").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Follower Coverage From Another Source"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Follower Coverage From Another Source").assertIsDisplayed()
    }
}
