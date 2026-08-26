package uk.sume.streamfolio.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import uk.sume.streamfolio.testutil.AndroidTestSupport
import uk.sume.streamfolio.testutil.BaseAndroidUiTest

/**
 * On-device tests for shared UI components reached from HomeScreen/DetailScreen (P2).
 *
 * Note: [SwipeableCard] is *not* tested here despite being listed in the original P2 backlog -
 * it turned out to be dead code (defined, never referenced from anywhere in the app). The actual
 * swipe-to-bookmark/share mechanism on HomeScreen's article cards is Material3's
 * `SwipeToDismissBox`, used inline in `HomeScreen.ArticleListItem` - that's what's exercised
 * below instead. [GlassmorphicNavBar] has no dedicated test file: every screen test in this
 * pass already navigates through it by tapping its tab labels, which is equivalent coverage of
 * its actual behavior (tab selection routing) without a redundant separate test.
 */
@RunWith(AndroidJUnit4::class)
class SharedComponentsTest : BaseAndroidUiTest() {

    @Test
    fun savingAnArticleFromDetailScreenOpensSaveToCollectionAndBookmarksIt() {
        // Covers the same NewsViewModel.toggleBookmark() -> SaveToListBottomSheet -> "Done" ->
        // toast flow that HomeScreen's swipe-to-bookmark gesture also calls (executeSwipeAction
        // "bookmark" -> handleBookmarkToggle() -> toggleBookmark()). The swipe gesture itself
        // (Material3 SwipeToDismissBox nested inside a vertically-scrolling LazyColumn) was not
        // reliably reproducible via performTouchInput's synthetic gestures in this environment -
        // DetailScreen's plain "Save" IconButton exercises the identical downstream flow via a
        // simple click instead.
        val link = "https://example.com/shared-component-test/save"
        AndroidTestSupport.seedCustomFeedArticle(link = link, title = "Detail Save Button Article", category = "Top Stories")

        launchApp()
        composeRule.waitUntilAtLeastOneExists(hasText("Detail Save Button Article"), timeoutMillis = 20_000)
        composeRule.onNodeWithTag("article_item_$link").performClick()
        composeRule.waitUntilAtLeastOneExists(hasContentDescription("Save"), timeoutMillis = 10_000)

        composeRule.onNodeWithContentDescription("Save").performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("Save to Collection"), timeoutMillis = 10_000)
        // The default list's Checkbox only becomes checked once SaveToListBottomSheet's own
        // LaunchedEffect(assignedListIds, allLists) finishes pre-selecting it; clicking "Done"
        // before that leaves selectedListIds empty, silently skipping both the DB write and the
        // toast (NewsViewModel.saveArticleToLists() only acts `if (targetListIds.isNotEmpty())`).
        composeRule.waitUntilAtLeastOneExists(isOn(), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Done").performClick()

        // hasText()/onNodeWithText() default to an *exact* match, not substring - the toast's
        // real text is the longer `Saved to "Saved"`.
        composeRule.waitUntilAtLeastOneExists(hasText("Saved to", substring = true), timeoutMillis = 10_000)
        val bookmarked = runBlocking { AndroidTestSupport.db().articleDao().getArticleByLink(link) }
        assertTrue("expected the article to be bookmarked", bookmarked?.isBookmarked == true)
    }

    @Test
    fun queueingAnArticleFromDetailScreenShowsTheMiniPlayerOnHome() {
        val link = "https://example.com/shared-component-test/queue"
        AndroidTestSupport.seedCustomFeedArticle(link = link, title = "Queued Playback Article", category = "Top Stories")

        launchApp()
        composeRule.waitUntilAtLeastOneExists(hasText("Queued Playback Article"), timeoutMillis = 20_000)
        composeRule.onNodeWithTag("article_item_$link").performClick()
        composeRule.waitUntilAtLeastOneExists(hasContentDescription("Back"), timeoutMillis = 10_000)

        composeRule.onNodeWithContentDescription("Queue in Playlist").performClick()

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.waitUntilAtLeastOneExists(hasTestTag("tts_mini_player"), timeoutMillis = 10_000)
        composeRule.onNodeWithTag("tts_mini_player").assertIsDisplayed()
    }
}
