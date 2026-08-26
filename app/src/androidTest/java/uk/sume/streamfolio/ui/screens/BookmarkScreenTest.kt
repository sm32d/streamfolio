package uk.sume.streamfolio.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
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
 * On-device Compose UI tests for BookmarkScreen (P2). resetAppState() always seeds a default
 * "Saved" BookmarkList (mirroring AppDatabase's real onOpen invariant, see AndroidTestSupport's
 * kdoc) so BookmarkScreen's `bookmarkedArticles.isEmpty() && allLists.isEmpty()` EmptyState
 * branch is unreachable here - every test lands on the grid/list collection view.
 */
@RunWith(AndroidJUnit4::class)
class BookmarkScreenTest : BaseAndroidUiTest() {

    private fun navigateToBookmarksTab() {
        composeRule.waitUntilAtLeastOneExists(hasText("Saved"), timeoutMillis = 20_000)
        composeRule.onNodeWithText("Saved").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Your saved collections and reads"), timeoutMillis = 10_000)
    }

    @Test
    fun bookmarkScreenShowsAllSavedCollectionCard() {
        launchApp()
        navigateToBookmarksTab()

        // Not asserting on the default list's own "Saved" title text here: it collides with the
        // bottom nav's "Saved" tab label, which is simultaneously on screen.
        composeRule.onNodeWithText("All Saved").assertIsDisplayed()
    }

    @Test
    fun togglingViewModeSwitchesBetweenGridAndList() {
        launchApp()
        navigateToBookmarksTab()

        // bookmarkListViewModeIsGrid defaults to true, so the toggle starts offering "List View".
        composeRule.waitUntilAtLeastOneExists(hasContentDescription("Switch to List View"), timeoutMillis = 10_000)
        composeRule.onNodeWithContentDescription("Switch to List View").performClick()

        composeRule.waitUntilAtLeastOneExists(hasContentDescription("Switch to Grid View"), timeoutMillis = 10_000)
        composeRule.onNodeWithContentDescription("Switch to Grid View").assertIsDisplayed()
    }

    @Test
    fun creatingACollectionAddsItToTheCollectionsList() {
        launchApp()
        navigateToBookmarksTab()

        composeRule.onNodeWithContentDescription("Create Collection").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Create New Collection"), timeoutMillis = 10_000)

        composeRule.onNodeWithTag("create_collection_name_field").performTextInput("Weekend Reading")
        composeRule.onNodeWithText("Create").performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("Weekend Reading"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Weekend Reading").assertIsDisplayed()
    }

    @Test
    fun tappingAllSavedOpensCollectionDetailShowingABookmarkedArticle() {
        AndroidTestSupport.seedArticle(
            AndroidTestSupport.article(
                link = "https://example.com/bookmark-test/saved-1",
                title = "Bookmarked Article For Collection Detail Test",
                isBookmarked = true
            )
        )

        launchApp()
        navigateToBookmarksTab()

        composeRule.waitUntilAtLeastOneExists(hasText("All Saved"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("All Saved").performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("Bookmarked Article For Collection Detail Test"), timeoutMillis = 10_000)
        composeRule.onNodeWithText("Bookmarked Article For Collection Detail Test").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }
}
